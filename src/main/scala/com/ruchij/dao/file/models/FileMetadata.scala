package com.ruchij.dao.file.models

import org.http4s.MediaType
import org.joda.time.DateTime

case class FileMetadata(fileId: String, createdAt: DateTime, path: String, mediaType: MediaType, size: Long)
