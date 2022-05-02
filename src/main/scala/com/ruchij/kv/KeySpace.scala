package com.ruchij.kv

import cats.{Applicative, ApplicativeError}
import com.ruchij.circe.Decoders.{authenticationTokenDecoder, dateTimeDecoder}
import com.ruchij.circe.Encoders.{authenticationTokenEncoder, dateTimeEncoder}
import com.ruchij.kv.codecs.{KVDecoder, KVEncoder}
import com.ruchij.services.authentication.models.{AuthenticationToken, AuthenticationTokenDetails}
import io.circe.generic.auto.{exportDecoder, exportEncoder}
import io.circe.{Decoder, Encoder}

abstract class KeySpace[K, V](
  implicit private val keyCirceEncoder: Encoder[K],
  private val valueCirceEncoder: Encoder[V],
  private val valueCirceDecoder: Decoder[V]
) {
  val name: String

  implicit def keyEncoder[F[_]: Applicative]: KVEncoder[F, K] = KVEncoder.lift[F, K]

  implicit def valueEncoder[F[_]: Applicative]: KVEncoder[F, V] = KVEncoder.lift[F, V]

  implicit def valueDecoder[F[_]: ApplicativeError[*[_], Throwable]]: KVDecoder[F, V] = KVDecoder.lift[F, V]

}

object KeySpace {
  case object AuthenticationKeySpace extends KeySpace[AuthenticationToken, AuthenticationTokenDetails] {
    override val name: String = "authentication"
  }
}
