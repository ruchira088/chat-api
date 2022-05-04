package com.ruchij.web.routes

import cats.data.OptionT
import cats.effect.{Async, Deferred}
import cats.implicits._
import com.ruchij.circe.Encoders.{dateTimeEncoder, enumEncoder}
import com.ruchij.dao.user.models.User
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.messages.MessagingService
import com.ruchij.services.messages.models.UserMessage
import com.ruchij.types.FunctionKTypes._
import com.ruchij.types.{JodaClock, Logger}
import com.ruchij.web.ws.{InboundMessage, OutboundMessage}
import fs2.Stream
import fs2.concurrent.Channel
import io.circe.{Encoder, parser => JsonParser}
import io.circe.generic.auto.exportEncoder
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame

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
                channel <- Stream.eval(Channel.unbounded[F, UserMessage])
                _ <- Stream.eval(messagingService.register(user, channel))
                userMessage <- channel.stream
              } yield WebSocketFrame.Text(Encoder[OutboundMessage].apply(OutboundMessage.fromUserMessage(userMessage)).noSpaces, last = false),
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
                    case InboundMessage.Authentication(_, authenticationToken) =>
                      Stream.eval { authenticationService.authenticate(authenticationToken).flatMap(deferred.complete) }
                        .productR(Stream.empty)

                    case inboundMessage =>
                      messagingService.submit {
                        Stream.eval(deferred.get.product(JodaClock[F].timestamp))
                          .flatMap { case (user, timestamp) =>
                            Stream.emits(InboundMessage.toUserMessage(user.id, inboundMessage, timestamp).toList)
                          }
                      }

                }
            )
        }

    }
  }

}
