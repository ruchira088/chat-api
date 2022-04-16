package com.ruchij.web.routes

import cats.data.OptionT
import cats.effect.{Async, Deferred}
import cats.implicits._
import com.ruchij.dao.user.models.User
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.messages.MessagingService
import com.ruchij.services.messages.models.Message
import io.circe.{Encoder, parser => JsonParser}
import fs2.Stream
import fs2.concurrent.Channel
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import com.ruchij.types.FunctionKTypes._

import scala.language.postfixOps

object WebSocketRoutes {

  def apply[F[_]: Async](
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
                message <- channel.stream
              } yield WebSocketFrame.Text(Encoder[Message].apply(message).noSpaces, last = false),
              input =>
                input
                  .evalMap {
                    case WebSocketFrame.Text(text, _) =>
                      JsonParser
                        .parse(text)
                        .flatMap(_.as[Message])
                        .toType[F, Throwable]
                  }
                  .evalMap {
                    case Message.Authentication(authenticationToken) =>
                      authenticationService.authenticate(authenticationToken)
                        .flatMap { user => deferred.complete(user).as((): Unit) }
                }
            )
        }

    }
  }

}
