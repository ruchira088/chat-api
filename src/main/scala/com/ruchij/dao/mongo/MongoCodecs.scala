package com.ruchij.dao.mongo

import cats.Show
import org.bson.codecs.{Codec, DecoderContext, EncoderContext}
import org.bson.{BsonReader, BsonWriter}
import org.http4s.MediaType
import org.joda.time.DateTime

object MongoCodecs {

  val dateTimeCodec: Codec[DateTime] =
    new Codec[DateTime] {
      override def encode(writer: BsonWriter, dateTime: DateTime, encoderContext: EncoderContext): Unit =
        writer.writeDateTime(dateTime.getMillis)

      override def getEncoderClass: Class[DateTime] = classOf[DateTime]

      override def decode(reader: BsonReader, decoderContext: DecoderContext): DateTime =
        new DateTime(reader.readDateTime())
    }

  val mediaTypeCodec: Codec[MediaType] =
    new Codec[MediaType] {
      override def encode(writer: BsonWriter, mediaType: MediaType, encoderContext: EncoderContext): Unit =
        writer.writeString(Show[MediaType].show(mediaType))

      override def getEncoderClass: Class[MediaType] = classOf[MediaType]

      override def decode(reader: BsonReader, decoderContext: DecoderContext): MediaType =
        MediaType.unsafeParse(reader.readString())
    }

}
