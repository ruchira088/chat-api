package com.ruchij.web.routes

import cats.effect.Async
import cats.effect.kernel.Sync
import fs2.Stream
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame.Text

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object SessionRoutes {

  def apply[F[_]: Async](webSocketBuilder2: WebSocketBuilder2[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root =>
        webSocketBuilder2
          .withOnClose {
            Sync[F].delay {
              println("Closing WS connection")
            }
          }
          .build(
            Stream[F, Int](1)
              .repeat
              .scan(0) { case (acc, value) => acc + value }
              .map { value =>
                Text(value.toString)
              }
              .metered(1 seconds),
            input => ???
          )
    }
  }

}
