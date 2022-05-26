package com.ruchij.services.filestore.models

import fs2.Stream
import org.http4s.MediaType
import org.joda.time.DateTime

case class FileResource[F[_]](
  fileId: String,
  createdAt: DateTime,
  path: String,
  mediaType: MediaType,
  size: Long,
  data: Stream[F, Byte]
)
