package com.ruchij.web

import cats.effect.Async
import cats.implicits.toSemigroupKOps
import com.ruchij.config.AuthenticationConfiguration.ServiceAuthenticationConfiguration
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.filestore.FileStore
import com.ruchij.services.health.HealthService
import com.ruchij.services.messages.MessagingService
import com.ruchij.services.user.UserService
import com.ruchij.types.JodaClock
import com.ruchij.web.middleware.{ExceptionHandler, NotFoundHandler}
import com.ruchij.web.routes._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.server.middleware.GZip
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.{HttpApp, HttpRoutes}

object Routes {
  def apply[F[_]: Async: JodaClock](
    userService: UserService[F],
    authenticationService: AuthenticationService[F],
    messagingService: MessagingService[F],
    healthService: HealthService[F],
    webSocketBuilder2: WebSocketBuilder2[F],
    fileStore: FileStore[F],
    serviceAuthenticationConfiguration: ServiceAuthenticationConfiguration
  ): HttpApp[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}

    val routes: HttpRoutes[F] =
      ResourceRoutes[F] <+>
        Router(
          "/user" -> UserRoutes(userService, authenticationService),
          "/authentication" -> AuthenticationRoutes(authenticationService),
          "/ws" -> WebSocketRoutes(messagingService, authenticationService, webSocketBuilder2),
          "/push" -> PushRoutes(messagingService, serviceAuthenticationConfiguration),
          "/file-resource" -> FileResourceRoutes(fileStore, authenticationService),
          "/service" -> HealthRoutes(healthService)
        )

    GZip {
      ExceptionHandler {
        NotFoundHandler(routes)
      }
    }
  }
}
