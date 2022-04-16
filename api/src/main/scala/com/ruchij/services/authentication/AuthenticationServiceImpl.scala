package com.ruchij.services.authentication

import cats.{Applicative, ApplicativeError, MonadThrow, ~>}
import cats.implicits._
import cats.data.OptionT
import com.ruchij.dao.credentials.CredentialsDao
import com.ruchij.dao.user.UserDao
import com.ruchij.dao.user.models.{Email, User}
import com.ruchij.exceptions.AuthenticationException
import com.ruchij.kv.KeySpacedKeyValueStore
import com.ruchij.services.authentication.AuthenticationService.SessionDuration
import com.ruchij.services.authentication.models.{AuthenticationToken, AuthenticationTokenDetails, Password}
import com.ruchij.services.hashing.PasswordHashingService
import com.ruchij.types.{JodaClock, RandomGenerator}
import org.joda.time.DateTime

import java.util.UUID

class AuthenticationServiceImpl[F[_]: MonadThrow: JodaClock: RandomGenerator[*[_], UUID], G[_]: MonadThrow](
  keySpacedKeyValueStore: KeySpacedKeyValueStore[F, AuthenticationToken, AuthenticationTokenDetails],
  passwordHashingService: PasswordHashingService[F],
  userDao: UserDao[G],
  credentialsDao: CredentialsDao[G]
)(implicit transaction: G ~> F)
    extends AuthenticationService[F] {

  override def login(email: Email, password: Password): F[AuthenticationToken] =
    transaction {
      OptionT(userDao.findByEmail(email))
        .flatMapF(user => credentialsDao.findByUserId(user.id))
        .getOrElseF(ApplicativeError[G, Throwable].raiseError(AuthenticationException("User not found")))
    }
      .flatMap { credentials =>
        for {
          isPasswordMatch <- passwordHashingService.checkPassword(password, credentials.saltedHashedPassword)
          _ <-
            if (isPasswordMatch) Applicative[F].unit
            else ApplicativeError[F, Throwable].raiseError(AuthenticationException("Invalid credentials"))

          token <- RandomGenerator[F, UUID].generate
          timestamp <- JodaClock[F].timestamp
          expiryTime = timestamp.plus(SessionDuration.length)

          authenticationToken = AuthenticationToken(token.toString)
          authenticationTokenDetails =
            AuthenticationTokenDetails(credentials.userId, authenticationToken, timestamp, expiryTime, 0)

          _ <- keySpacedKeyValueStore.insert(authenticationToken, authenticationTokenDetails)
        }
        yield authenticationToken
      }

  override def authenticate(authenticationToken: AuthenticationToken): F[User] =
    OptionT(keySpacedKeyValueStore.find(authenticationToken))
      .getOrElseF(ApplicativeError[F, Throwable].raiseError(AuthenticationException("Invalid authentication token")))
      .flatMap { authenticationTokenDetails =>
        JodaClock[F].timestamp
          .flatMap { timestamp =>
            if (timestamp.isBefore(authenticationTokenDetails.expiresAt))
              Applicative[F].pure(timestamp.plus(SessionDuration.length))
            else ApplicativeError[F, Throwable].raiseError[DateTime](AuthenticationException("Expired authentication token"))
          }
          .flatMap { updatedExpiryTime =>
            keySpacedKeyValueStore.insert(
              authenticationToken,
              authenticationTokenDetails
                .copy(expiresAt = updatedExpiryTime, renewals = authenticationTokenDetails.renewals + 1)
            )
          }
          .productR {
            OptionT(transaction(userDao.findByUserId(authenticationTokenDetails.userId)))
              .getOrElseF(ApplicativeError[F, Throwable].raiseError(AuthenticationException("User not found")))
          }
      }

  override def logout(authenticationToken: AuthenticationToken): F[Boolean] = ???

}
