package com.ruchij.web.routes

import cats.effect.kernel.Async
import cats.implicits._
import com.ruchij.circe.Decoders.{emailDecoder, passwordDecoder}
import com.ruchij.circe.Encoders.{dateTimeEncoder, emailEncoder}
import com.ruchij.dao.user.models.User
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.user.UserService
import com.ruchij.web.middleware.UserAuthenticator
import com.ruchij.web.requests.QueryParameters.{PageNumber, PageSize, SearchTerm}
import com.ruchij.web.requests.{CreateUserRequest, ProfileImageRequest, RequestOps}
import com.ruchij.web.responses.UserSearchResponse
import com.ruchij.web.validate.Validator.baseValidator
import io.circe.generic.auto._
import org.http4s.{ContextRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object UserRoutes {
  def apply[F[_]: Async](userService: UserService[F], authenticationService: AuthenticationService[F])(
    implicit dsl: Http4sDsl[F]
  ): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of[F] {
      case request @ POST -> Root =>
        for {
          createUserRequest <- request.to[CreateUserRequest]
          user <- userService.create(
            createUserRequest.firstName,
            createUserRequest.lastName,
            createUserRequest.email,
            createUserRequest.password
          )

          response <- Created(user)
        } yield response
    } <+>
      UserAuthenticator(authenticationService).apply {
        ContextRoutes.of[User, F] {
          case GET -> Root / "search" :? SearchTerm(searchTerm) +& PageNumber(pageNumber) +& PageSize(pageSize) as _ =>
            userService.searchUsers(searchTerm, pageSize, pageNumber)
              .flatMap { users =>
                Ok(UserSearchResponse(searchTerm, pageNumber, pageSize, users))
              }

          case (request @ POST -> Root / "profile-image") as user =>
            for {
              profileImageRequest <- request.as[ProfileImageRequest[F]]
            } yield ???
        }
      }
  }
}
