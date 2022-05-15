package com.ruchij.web.routes

import cats.effect.Async
import cats.implicits._
import com.ruchij.circe.Encoders.{dateTimeEncoder, enumEncoder}
import com.ruchij.dao.user.models.User
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.messages.MessagingService
import com.ruchij.services.messages.models.Message
import com.ruchij.services.messages.models.Message.{HeartBeat, MessageAcknowledgement}
import com.ruchij.types.FunctionKTypes._
import com.ruchij.types.{JodaClock, Logger}
import com.ruchij.web.middleware.UserAuthenticator
import com.ruchij.web.ws.InboundMessage.inboundWebSocketMessageDecoder
import com.ruchij.web.ws.{InboundMessage, OutboundMessage}
import fs2.Stream
import fs2.concurrent.Channel
import io.circe.generic.auto.exportEncoder
import io.circe.{Encoder, parser => JsonParser}
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.{ContextRoutes, HttpRoutes}

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

    UserAuthenticator(authenticationService).apply {
      ContextRoutes.of[User, F] {
        case GET -> Root as user =>
          webSocketBuilder2
            .withOnClose {
              messagingService.unregister(user).as((): Unit)
            }
            .build(
              for {
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
                    inboundMessage =>
                      Stream.eval(JodaClock[F].timestamp)
                        .flatMap { timestamp =>
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
