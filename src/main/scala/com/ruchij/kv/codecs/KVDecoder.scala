package com.ruchij.kv.codecs

import cats.{ApplicativeError, ~>}
import com.ruchij.types.FunctionKTypes.{FunctionK2TypeOps, eitherToF}
import io.circe.{Decoder, parser => JsonParser}

trait KVDecoder[F[_], +A] { self =>
  def decode[C >: A](value: String): F[C]

  def mapK[G[_]](transform: F ~> G): KVDecoder[G, A] =
    new KVDecoder[G, A] {
      override def decode[C >: A](value: String): G[C] = transform(self.decode(value))
    }
}

object KVDecoder {
  def apply[F[_], A](implicit kvDecoder: KVDecoder[F, A]): KVDecoder[F, A] = kvDecoder

  implicit def lift[F[_]: ApplicativeError[*[_], Throwable], A](implicit decoder: Decoder[A]): KVDecoder[F, A] =
    new KVDecoder[F, A] {
      override def decode[C >: A](input: String): F[C] =
        JsonParser.parse(input).flatMap[Throwable, C](json => decoder.decodeJson(json)).toType[F, Throwable]
    }
}