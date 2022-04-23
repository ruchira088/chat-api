package com.ruchij.web.routes

import cats.effect.Async
import cats.implicits._
import com.ruchij.circe.Decoders.{emailDecoder, passwordDecoder}
import com.ruchij.circe.Encoders.authenticationTokenEncoder
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.web.requests.{RequestOps, UserLoginRequest}
import com.ruchij.web.responses.AuthenticationTokenResponse
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object AuthenticationRoutes {

  def apply[F[_]: Async](authenticationService: AuthenticationService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of {
      case request @ POST -> Root =>
        for {
          UserLoginRequest(email, password) <- request.to[UserLoginRequest]
          authenticationToken <- authenticationService.login(email, password)
          response <- Created(AuthenticationTokenResponse(authenticationToken))
        } yield response
    }

  }

}
