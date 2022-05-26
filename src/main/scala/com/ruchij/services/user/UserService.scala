package com.ruchij.services.user

import com.ruchij.dao.user.models.{Email, User}
import com.ruchij.services.authentication.models.Password
import fs2.Stream
import org.http4s.MediaType

trait UserService[F[_]] {
  def create(firstName: String, lastName: String, email: Email, password: Password): F[User]

  def addProfileImage(userId: String, fileName: String, mediaType: MediaType, profileImage: Stream[F, Byte])

  def searchUsers(searchTerm: String, pageSize: Int, pageNumber: Int): F[Seq[User]]
}