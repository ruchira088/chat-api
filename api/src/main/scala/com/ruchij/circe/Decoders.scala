package com.ruchij.circe

import com.ruchij.dao.user.models.Email
import com.ruchij.services.authentication.models.{AuthenticationToken, Password}
import io.circe.Decoder
import org.joda.time.DateTime
import shapeless.{::, Generic, HNil}

import scala.util.Try

object Decoders {
  implicit val dateTimeDecoder: Decoder[DateTime] =
    Decoder.decodeString.emapTry(dateTimeString => Try(DateTime.parse(dateTimeString)))

  private def stringWrapperDecoder[A](implicit generic: Generic.Aux[A, String :: HNil]): Decoder[A] =
    Decoder.decodeString.emap { value =>
      if (value.trim.isEmpty) Left("Cannot be empty") else Right(generic.from(value :: HNil))
    }

  implicit val passwordDecoder: Decoder[Password] = stringWrapperDecoder[Password]

  implicit val emailDecoder: Decoder[Email] = stringWrapperDecoder[Email]

  implicit val authenticationTokenDecoder: Decoder[AuthenticationToken] = stringWrapperDecoder[AuthenticationToken]

}
