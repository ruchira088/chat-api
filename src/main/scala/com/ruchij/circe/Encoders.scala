package com.ruchij.circe

import com.ruchij.dao.user.models.Email
import com.ruchij.services.authentication.models.AuthenticationToken
import io.circe.Encoder
import org.joda.time.DateTime
import shapeless.{::, Generic, HNil}

object Encoders {
  implicit val dateTimeEncoder: Encoder[DateTime] = Encoder.encodeString.contramap[DateTime](_.toString)

  private def stringWrapperEncoder[A](implicit generic: Generic.Aux[A, String :: HNil]): Encoder[A] =
    Encoder.encodeString.contramap[A] { value => generic.to(value).head }

  implicit val emailEncoder: Encoder[Email] = stringWrapperEncoder[Email]

  implicit val authenticationTokenEncoder: Encoder[AuthenticationToken] = stringWrapperEncoder[AuthenticationToken]
}
