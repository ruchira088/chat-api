package com.ruchij.services.authentication.models

import cats.{Applicative, ApplicativeError}
import cats.implicits._
import com.ruchij.kv.codecs.{KVDecoder, KVEncoder}
import com.ruchij.types.FunctionKTypes.{eitherToF, FunctionK2TypeOps}
import io.circe.{Decoder, Encoder, parser => JsonParser}
import org.joda.time.DateTime

case class AuthenticationTokenDetails(
  userId: String,
  authenticationToken: AuthenticationToken,
  issuedAt: DateTime,
  expiresAt: DateTime,
  renewals: Long
)

object AuthenticationTokenDetails {

  implicit def authenticationTokenDetailsKVEncoder[F[_]: Applicative](
    implicit encoder: Encoder[AuthenticationTokenDetails]
  ): KVEncoder[F, AuthenticationTokenDetails] =
    new KVEncoder[F, AuthenticationTokenDetails] {
      override def encode[C <: AuthenticationTokenDetails](input: C): F[String] =
        Applicative[F].pure(encoder.apply(input).noSpaces)
    }

  implicit def authenticationTokenDetailsKVDecoder[F[_]: ApplicativeError[*[_], Throwable]](
    implicit decoder: Decoder[AuthenticationTokenDetails]
  ): KVDecoder[F, AuthenticationTokenDetails] =
    new KVDecoder[F, AuthenticationTokenDetails] {
      override def decode[C >: AuthenticationTokenDetails](value: String): F[C] =
        JsonParser.parse(value).flatMap(decoder.decodeJson).toType[F, Throwable].map(identity[C])
    }

}
