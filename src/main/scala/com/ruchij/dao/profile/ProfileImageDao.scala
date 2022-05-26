package com.ruchij.dao.profile

trait ProfileImageDao[F[_]] {
  def insert(userId: String, fileId: String): F[Int]
}