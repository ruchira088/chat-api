package com.ruchij.services.hashing

import com.ruchij.dao.credentials.models.SaltedHashValue
import com.ruchij.services.authentication.models.Password

trait PasswordHashingService[F[_]] {
  def hash(password: Password): F[SaltedHashValue]

  def checkPassword(password: Password, saltedHashValue: SaltedHashValue): F[Boolean]
}
