package com.ruchij.dao.credentials

import com.ruchij.dao.credentials.models.Credentials

trait CredentialsDao[F[_]] {
  def insert(credentials: Credentials): F[Int]

  def findByUserId(userId: String): F[Option[Credentials]]
}
