package com.ruchij.circe

import io.circe.Encoder
import org.joda.time.DateTime
import shapeless.{::, Generic, HNil}

object Encoders {
  implicit val dateTimeEncoder: Encoder[DateTime] = Encoder.encodeString.contramap[DateTime](_.toString)

  def stringWrapperEncoder[A](implicit generic: Generic.Aux[A, String :: HNil]): Encoder[A] =
    Encoder.encodeString.contramap[A] { value => generic.to(value).head }
}
