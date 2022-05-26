package com.ruchij.services.filestore

import com.ruchij.services.filestore.models.{FileInsertionResult, FileResource}
import fs2.Stream
import org.http4s.MediaType

trait FileStore[F[_]] {
  def save(fileName: String, mediaType: MediaType, data: Stream[F, Byte]): F[FileInsertionResult]

  def retrieve(fileId: String): F[FileResource[F]]
}
