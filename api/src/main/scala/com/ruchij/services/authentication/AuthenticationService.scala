package com.ruchij.services.authentication

import com.ruchij.dao.user.models.{Email, User}
import com.ruchij.services.authentication.models.{AuthenticationToken, Password}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.language.postfixOps

trait AuthenticationService[F[_]] {
  def login(email: Email, password: Password): F[AuthenticationToken]

  def authenticate(authenticationToken: AuthenticationToken): F[User]

  def logout(authenticationToken: AuthenticationToken): F[Boolean]
}

object AuthenticationService {
  val SessionDuration: FiniteDuration = 30 minutes
}
