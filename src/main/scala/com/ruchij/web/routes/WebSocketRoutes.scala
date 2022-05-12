package com.ruchij.web.routes

import cats.{Applicative, ApplicativeError}
import cats.data.OptionT
import cats.effect.{Async, Concurrent, Deferred}
import cats.implicits._
import com.ruchij.circe.Encoders.{dateTimeEncoder, enumEncoder}
import com.ruchij.dao.user.models.User
import com.ruchij.exceptions.AuthenticationException
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.messages.MessagingService
import com.ruchij.services.messages.models.Message
import com.ruchij.services.messages.models.Message.{HeartBeat, MessageAcknowledgement, messageCirceEncoder}
import com.ruchij.types.FunctionKTypes._
import com.ruchij.types.{JodaClock, Logger}
import com.ruchij.web.ws.{InboundMessage, OutboundMessage}
import com.ruchij.web.ws.InboundMessage.inboundWebSocketMessageDecoder
import fs2.Stream
import fs2.concurrent.Channel
import io.circe.{Encoder, parser => JsonParser}
import io.circe.generic.auto.exportEncoder
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object WebSocketRoutes {

  private val logger = Logger[WebSocketRoutes.type]

  def apply[F[_]: Async: JodaClock](
    messagingService: MessagingService[F],
    authenticationService: AuthenticationService[F],
    webSocketBuilder2: WebSocketBuilder2[F]
  )(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root =>
        Deferred[F, User].flatMap { deferred =>
          webSocketBuilder2
            .withOnClose {
              OptionT(deferred.tryGet).semiflatTap(messagingService.unregister).value.as((): Unit)
            }
            .build(
              for {
                user <- Stream.eval(deferred.get)
                channel <- Stream.eval(Channel.unbounded[F, Message])
                _ <- Stream.eval(messagingService.register(user, channel))

                message <-
                  channel.stream.merge {
                    Stream.eval(JodaClock[F].timestamp)
                      .repeat
                      .metered(30 seconds)
                      .map { dateTime => HeartBeat("heart-beat", dateTime) }
                  }

                webSocketFrame =
                  WebSocketFrame.Text {
                    Encoder[OutboundMessage].apply(OutboundMessage.fromMessage(message)).noSpaces
                  }
              } yield webSocketFrame,
              input =>
                input
                  .flatMap {
                    case WebSocketFrame.Text(text, _) =>
                      Stream.eval[F, InboundMessage](JsonParser.parse(text).flatMap(_.as[InboundMessage]).toType[F, Throwable])
                        .handleErrorWith { throwable =>
                          Stream.eval(logger.error("Unable to decode web socket message", throwable))
                            .productR(Stream.empty)
                        }

                    case _ => Stream.empty
                  }
                  .flatMap {
                    case InboundMessage.Authentication(messageId, authenticationToken) =>
                      Stream.eval {
                        authenticationService.authenticate(authenticationToken)
                          .flatTap(deferred.complete)
                      }
                        .evalMap { user =>
                          val authenticationAcknowledgement =
                              Stream.eval(JodaClock[F].timestamp)
                              .evalMap { timestamp =>
                                messagingService.sendToUser(user.id, MessageAcknowledgement(messageId, timestamp))
                              }
                              .metered(200 microseconds)
                              .filter(sent => sent)
                              .take(1)
                              .compile
                              .drain

                          Concurrent[F]
                            .race(Concurrent[F].sleep(5 seconds), authenticationAcknowledgement)
                            .flatMap {
                              _.fold[F[Unit]](
                                _ => ApplicativeError[F, Throwable].raiseError[Unit] {
                                  AuthenticationException("Authentication acknowledgement timeout after 5 seconds")
                                },
                                _ => Applicative[F].unit
                              )
                            }
                        }
                        .productR(Stream.empty)

                    case inboundMessage =>
                      Stream.eval(deferred.get.product(JodaClock[F].timestamp))
                        .flatMap { case (user, timestamp) =>
                          messagingService.submit {
                            Stream.emits(InboundMessage.toMessage(user.id, inboundMessage, timestamp).toList)
                          }
                            .evalTap { _ =>
                              messagingService.sendToUser(user.id, MessageAcknowledgement(inboundMessage.messageId, timestamp))
                            }
                        }
                }
            )
        }

    }
  }

}
