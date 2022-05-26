package com.ruchij.dao.file

import cats.effect.kernel.Sync
import cats.implicits._
import cats.~>
import com.ruchij.dao.file.models.FileMetadata
import com.ruchij.types.FunctionKTypes.WrappedFuture
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.InsertOneResult

class MongoFileMetadataDao[F[_]: Sync](mongoDatabase: MongoDatabase)(
  implicit futureUnwrapper: WrappedFuture[F, *] ~> F
) extends FileMetadataDao[F] {
  private val mongoCollection = mongoDatabase.getCollection[FileMetadata]("file-metadata")

  override def insert(fileMetadata: FileMetadata): F[Int] = {
    val wrappedFuture: WrappedFuture[F, InsertOneResult] =
      Sync[F].blocking(mongoCollection.insertOne(fileMetadata).toFuture())

    futureUnwrapper(wrappedFuture).map(insertionResult => if (insertionResult.wasAcknowledged()) 1 else 0)
  }

  override def findById(fileId: String): F[Option[FileMetadata]] = {
    val wrappedFuture: WrappedFuture[F, Seq[FileMetadata]] =
      Sync[F].blocking(mongoCollection.find(equal("fileId", fileId)).limit(1).toFuture())

    futureUnwrapper(wrappedFuture).map(_.headOption)
  }

}
