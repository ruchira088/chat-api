package com.ruchij.web.middleware

import cats.{ApplicativeError, MonadThrow}
import cats.data.{Kleisli, OptionT}
import com.ruchij.dao.user.models.User
import com.ruchij.exceptions.AuthenticationException
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.authentication.models.AuthenticationToken
import org.http4s.headers.Authorization
import org.http4s.{ContextRequest, Credentials, Request, Response, ResponseCookie}
import org.http4s.server.AuthMiddleware
import org.typelevel.ci.CIStringSyntax

object UserAuthenticator {
  val AuthenticationCookieName = "authentication"
  val AuthTokenScheme = ci"Bearer"

  def apply[F[_]: MonadThrow](authenticationService: AuthenticationService[F]): AuthMiddleware[F, User] =
    authRoutes =>
      Kleisli[OptionT[F, *], Request[F], Response[F]] {
        request =>
          OptionT.fromOption(authenticationToken(request))
            .orElseF {
              ApplicativeError[F, Throwable].raiseError {
                AuthenticationException("Missing bearer token or authentication cookie")
              }
            }
            .semiflatMap { authenticationToken =>
              authenticationService.authenticate(authenticationToken)
            }
            .flatMap {
              user => authRoutes.run(ContextRequest(user, request))
            }
      }

  def authenticationToken[F[_]](request: Request[F]): Option[AuthenticationToken] =
    request.cookies
      .find(_.name == AuthenticationCookieName)
      .map(_.content)
      .orElse {
        request.headers.get[Authorization]
          .collect {
            case Authorization(Credentials.Token(AuthTokenScheme, token)) => token
          }
      }
      .map(AuthenticationToken.apply)

  def addAuthenticationCookie[F[_]](response: Response[F], authenticationToken: AuthenticationToken): Response[F] =
    response.addCookie {
      ResponseCookie(AuthenticationCookieName, authenticationToken.value)
    }

}
