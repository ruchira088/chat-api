package com.ruchij.dao.file

import com.ruchij.dao.file.models.FileMetadata

trait FileMetadataDao[F[_]] {
  def insert(fileMetadata: FileMetadata): F[Int]

  def findById(fileId: String): F[Option[FileMetadata]]
}
