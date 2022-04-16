package com.ruchij.services.hashing

import cats.effect.Sync
import cats.implicits._
import com.ruchij.dao.credentials.models.SaltedHashValue
import com.ruchij.services.authentication.models.Password
import org.mindrot.jbcrypt.BCrypt

class BcryptPasswordHashingService[F[_]: Sync] extends PasswordHashingService[F] {

  override def hash(password: Password): F[SaltedHashValue] =
    Sync[F].blocking(BCrypt.hashpw(password.value, BCrypt.gensalt()))
      .map(SaltedHashValue.apply)

  override def checkPassword(password: Password, saltedHashValue: SaltedHashValue): F[Boolean] =
    Sync[F].blocking(BCrypt.checkpw(password.value, saltedHashValue.value))

}
