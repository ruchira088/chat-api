package com.ruchij.kv.codecs

import cats.Applicative

trait KVEncoder[F[_], -A] {
  def encode[C <: A](input: C): F[String]
}

object KVEncoder {
  def apply[F[_], A](implicit kvEncoder: KVEncoder[F, A]): KVEncoder[F, A] = kvEncoder

  implicit def stringKVEncoder[F[_]: Applicative]: KVEncoder[F, String] =
    new KVEncoder[F, String] {
      override def encode[C <: String](input: C): F[String] = Applicative[F].pure(input)
    }
}
