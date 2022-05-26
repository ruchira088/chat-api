package com.ruchij.services.user

import cats.effect.kernel.Sync
import cats.implicits._
import cats.{Applicative, ApplicativeError, Monad, ~>}
import com.ruchij.dao.credentials.CredentialsDao
import com.ruchij.dao.credentials.models.Credentials
import com.ruchij.dao.profile.ProfileImageDao
import com.ruchij.dao.user.UserDao
import com.ruchij.dao.user.models.{Email, User}
import com.ruchij.exceptions.ResourceConflictException
import com.ruchij.services.authentication.models.Password
import com.ruchij.services.filestore.FileStore
import com.ruchij.services.filestore.models.FileInsertionResult
import com.ruchij.services.hashing.PasswordHashingService
import com.ruchij.types.RandomGenerator.IdGenerator
import com.ruchij.types.{JodaClock, RandomGenerator}
import fs2.Stream
import org.http4s.MediaType

import java.util.UUID

class UserServiceImpl[F[_]: Sync: JodaClock: IdGenerator, G[_]: Monad](
  passwordHashingService: PasswordHashingService[F],
  fileStore: FileStore[F],
  userDao: UserDao[G],
  credentialsDao: CredentialsDao[G],
  profileImageDao: ProfileImageDao[G]
)(implicit transaction: G ~> F)
    extends UserService[F] {

  override def create(firstName: String, lastName: String, email: Email, password: Password): F[User] =
    for {
      maybeExistingUser <- transaction(userDao.findByEmail(email))

      _ <- maybeExistingUser.fold(Applicative[F].unit) { _ =>
        ApplicativeError[F, Throwable].raiseError {
          ResourceConflictException {
            s"${email.value} already exists"
          }
        }
      }

      userId <- RandomGenerator[F, UUID].generate.map(_.toString)
      timestamp <- JodaClock[F].timestamp

      hashedPassword <- passwordHashingService.hash(password)

      user = User(userId, timestamp, email, firstName, lastName, None)
      credentials = Credentials(userId, timestamp, timestamp, hashedPassword)

      _ <- transaction { userDao.insert(user).product(credentialsDao.insert(credentials)) }

    } yield user

  override def searchUsers(searchTerm: String, pageSize: Int, pageNumber: Int): F[Seq[User]] =
    transaction { userDao.search(searchTerm, pageNumber, pageSize) }

  override def addProfileImage(
    userId: String,
    fileName: String,
    mediaType: MediaType,
    profileImage: Stream[F, Byte]
  ): F[FileInsertionResult] =
    for {
      insertionResult <- fileStore.save(fileName, mediaType, profileImage)
      _ <- transaction(profileImageDao.insert(userId, insertionResult.fileId))
    } yield insertionResult
}
