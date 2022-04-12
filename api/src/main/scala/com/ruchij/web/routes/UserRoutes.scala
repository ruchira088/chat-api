package com.ruchij.web.routes

import cats.effect.kernel.Async
import cats.implicits._
import com.ruchij.services.user.UserService
import com.ruchij.web.requests.{CreateUserRequest, RequestOps}
import com.ruchij.web.validate.Validator.baseValidator
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl

object UserRoutes {
  def apply[F[_]: Async](userService: UserService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes.of {
      case request @ POST -> Root =>
        for {
          createUserRequest <- request.to[CreateUserRequest]
          user <-
            userService.create(
              createUserRequest.firstName,
              createUserRequest.lastName,
              createUserRequest.email,
              createUserRequest.password
            )
        }
        yield Created(user)

    }
  }
}
