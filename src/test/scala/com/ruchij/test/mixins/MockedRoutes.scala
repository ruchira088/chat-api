package com.ruchij.test.mixins

import cats.effect.Async
import com.ruchij.config.AuthenticationConfiguration.ServiceAuthenticationConfiguration
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.health.HealthService
import com.ruchij.services.messages.MessagingService
import com.ruchij.services.user.UserService
import com.ruchij.web.Routes
import org.http4s.HttpApp
import org.http4s.server.websocket.WebSocketBuilder2
import org.scalamock.scalatest.MockFactory
import org.scalatest.OneInstancePerTest

trait MockedRoutes[F[_]] extends MockFactory with OneInstancePerTest {

  val userService: UserService[F] = mock[UserService[F]]
  val authenticationService: AuthenticationService[F] = mock[AuthenticationService[F]]
  val messagingService: MessagingService[F] = mock[MessagingService[F]]
  val healthService: HealthService[F] = mock[HealthService[F]]
  val webSocketBuilder2: WebSocketBuilder2[F] = null
  val serviceAuthenticationConfiguration = mock[ServiceAuthenticationConfiguration]

  val async: Async[F]

  def createRoutes(): HttpApp[F] =
    Routes[F](
      userService,
      authenticationService,
      messagingService,
      healthService,
      webSocketBuilder2,
      serviceAuthenticationConfiguration
    )(async)

}
