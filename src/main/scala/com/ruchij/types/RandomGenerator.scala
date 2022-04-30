package com.ruchij.types

import cats.effect.kernel.Sync

import java.util.UUID

trait RandomGenerator[F[_], +A] {
  def generate[B >: A]: F[B]
}

object RandomGenerator {
  type IdGenerator[F[_]] = RandomGenerator[F, UUID]

  def apply[F[_], A](implicit randomGenerator: RandomGenerator[F, A]): RandomGenerator[F, A] = randomGenerator

  def apply[F[_]: Sync, A](block: => A): RandomGenerator[F, A] =
    new RandomGenerator[F, A] {
      override def generate[B >: A]: F[B] = Sync[F].delay(block)
    }

  implicit def uuidGenerator[F[_]: Sync]: RandomGenerator[F, UUID] =
    new RandomGenerator[F, UUID] {
      override def generate[B >: UUID]: F[B] = Sync[F].delay(UUID.randomUUID())
    }
}
