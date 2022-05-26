package com.ruchij.services.filestore

import cats.data.OptionT
import cats.effect.kernel.Async
import cats.implicits._
import cats.~>
import com.ruchij.config.FileStoreConfiguration
import com.ruchij.dao.file.FileMetadataDao
import com.ruchij.dao.file.models.FileMetadata
import com.ruchij.services.filestore.models.{FileInsertionResult, FileResource}
import com.ruchij.types.RandomGenerator.IdGenerator
import com.ruchij.types.{JodaClock, RandomGenerator}
import fs2.Stream
import fs2.io.file.{Files, Path}
import org.http4s.MediaType

import java.util.UUID

class LocalFileStore[F[_]: Files: IdGenerator: JodaClock: Async, G[_]](
  fileStoreConfiguration: FileStoreConfiguration,
  fileMetadataDao: FileMetadataDao[G]
)(implicit transaction: G ~> F)
    extends FileStore[F] {

  override def save(fileName: String, mediaType: MediaType, data: Stream[F, Byte]): F[FileInsertionResult] =
    for {
      id <- RandomGenerator[F, UUID].generate.map(_.toString)
      timestamp <- JodaClock[F].timestamp

      path = Path(fileStoreConfiguration.root).resolve(s"$id-$fileName")

      _ <- Files[F].writeAll(path)(data).compile.drain
      size <- Files[F].size(path)

      _ <- transaction {
        fileMetadataDao.insert {
          FileMetadata(id, timestamp, path.toString, mediaType, size)
        }
      }

    } yield FileInsertionResult(id, timestamp, path.toString, size)

  override def retrieve(fileId: String): F[Option[FileResource[F]]] =
    OptionT(transaction(fileMetadataDao.findById(fileId))).map { fileMetadata =>
      FileResource(
        fileMetadata.fileId,
        fileMetadata.createdAt,
        fileMetadata.path,
        fileMetadata.mediaType,
        fileMetadata.size,
        Files[F].readAll(Path(fileMetadata.path))
      )
    }.value

}
