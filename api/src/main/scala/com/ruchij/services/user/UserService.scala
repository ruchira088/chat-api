package com.ruchij.services.user

import com.ruchij.dao.user.models.{Email, User}
import com.ruchij.services.authentication.models.Password

trait UserService[F[_]] {
  def create(firstName: String, lastName: String, email: Email, password: Password): F[User]
}
