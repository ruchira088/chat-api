package com.ruchij.kv.codecs

import cats.{Applicative, ~>}
import io.circe.Encoder

trait KVEncoder[F[_], -A] { self =>
  def encode[C <: A](input: C): F[String]

  def mapK[G[_]](transform: F ~> G): KVEncoder[G, A] =
    new KVEncoder[G, A] {
      override def encode[C <: A](input: C): G[String] = transform(self.encode(input))
    }
}

object KVEncoder {
  def apply[F[_], A](implicit kvEncoder: KVEncoder[F, A]): KVEncoder[F, A] = kvEncoder

  implicit def lift[F[_]: Applicative, A](implicit encoder: Encoder[A]): KVEncoder[F, A] =
    new KVEncoder[F, A] {
      override def encode[C <: A](input: C): F[String] = Applicative[F].pure(encoder(input).noSpaces)
    }
}
