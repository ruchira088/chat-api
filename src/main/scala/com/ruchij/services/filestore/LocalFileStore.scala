package com.ruchij.services.filestore

import cats.effect.kernel.Async
import cats.implicits._
import com.ruchij.config.FileStoreConfiguration
import com.ruchij.services.filestore.models.{FileInsertionResult, FileResource}
import com.ruchij.types.RandomGenerator.IdGenerator
import com.ruchij.types.{JodaClock, RandomGenerator}
import fs2.Stream
import fs2.io.file.{Files, Path}
import org.http4s.MediaType

import java.util.UUID

class LocalFileStore[F[_]: Files: IdGenerator: JodaClock: Async](fileStoreConfiguration: FileStoreConfiguration)
    extends FileStore[F] {

  override def save(fileName: String, mediaType: MediaType, data: Stream[F, Byte]): F[FileInsertionResult] =
    for {
      id <- RandomGenerator[F, UUID].generate.map(_.toString)
      timestamp <- JodaClock[F].timestamp

      path = Path(fileStoreConfiguration.root).resolve(s"$id-$fileName")

      _ <- Files[F].writeAll(path)(data).compile.drain
      size <- Files[F].size(path)
    }
    yield FileInsertionResult(id, timestamp, path.toString, size)

  override def retrieve(fileId: String): F[FileResource[F]] = ???

}
