package com.ruchij.services.authentication.models

import cats.Applicative
import com.ruchij.kv.codecs.KVEncoder

case class AuthenticationToken(value: String) extends AnyVal

object AuthenticationToken {
  implicit def authenticationTokenKVEncoder[F[_]: Applicative]: KVEncoder[F, AuthenticationToken] =
    new KVEncoder[F, AuthenticationToken] {
      override def encode[C <: AuthenticationToken](input: C): F[String] = Applicative[F].pure(input.value)
    }
}
