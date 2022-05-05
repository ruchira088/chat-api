package com.ruchij.web.middleware

import cats.data.{Kleisli, OptionT}
import cats.{Applicative, ApplicativeError, MonadThrow}
import com.ruchij.config.AuthenticationConfiguration.ServiceAuthenticationConfiguration
import com.ruchij.exceptions.AuthenticationException
import org.http4s.headers.Authorization
import org.http4s.server.HttpMiddleware
import org.http4s.{Credentials, Request, Response}
import org.typelevel.ci.CIStringSyntax

object ServiceAuthenticator {

  def apply[F[_]: MonadThrow](serviceAuthenticationConfiguration: ServiceAuthenticationConfiguration): HttpMiddleware[F] =
    httpRoutes =>
      Kleisli[OptionT[F, *], Request[F], Response[F]] { request =>
        for {
          token <-
            bearerToken(request)
              .fold(
                OptionT.liftF[F, String] {
                  ApplicativeError[F, Throwable]
                    .raiseError(AuthenticationException("Missing bearer authentication token"))
                }
              )(token => OptionT.pure(token))

          _ <-
            if (token == serviceAuthenticationConfiguration.token) OptionT.liftF(Applicative[F].unit)
            else OptionT.liftF[F, Unit] {
              ApplicativeError[F, Throwable].raiseError {
                AuthenticationException("Invalid credentials")
              }
            }

          response <- httpRoutes.run(request)

        } yield response
    }

  def bearerToken[F[_]](request: Request[F]): Option[String] =
    request.headers
      .get[Authorization]
      .map(_.credentials)
      .collect {
        case Credentials.Token(authScheme, token) if authScheme == ci"Bearer" => token
      }

}
