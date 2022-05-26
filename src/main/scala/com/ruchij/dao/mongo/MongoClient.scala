package com.ruchij.dao.mongo

import cats.effect.Resource
import cats.effect.kernel.Sync
import com.mongodb.{ConnectionString, MongoClientSettings}
import com.ruchij.config.MongoConfiguration
import com.ruchij.dao.file.models.FileMetadata
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{MongoDatabase, MongoClient => Client}

object MongoClient {
  def create[F[_]: Sync](mongoConfiguration: MongoConfiguration): Resource[F, MongoDatabase] =
    Resource
      .make {
        Sync[F].blocking {
          Client(
            MongoClientSettings
              .builder()
              .applyConnectionString(new ConnectionString(mongoConfiguration.connectionString))
              .codecRegistry(
                fromRegistries(
                  fromCodecs(MongoCodecs.dateTimeCodec, MongoCodecs.mediaTypeCodec),
                  fromProviders(classOf[FileMetadata]),
                  Client.DEFAULT_CODEC_REGISTRY
                )
              )
              .build()
          )
        }
      } { client =>
        Sync[F].blocking(client.close())
      }
      .map(_.getDatabase(mongoConfiguration.database))
}
