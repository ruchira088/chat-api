package com.ruchij.services.filestore.models

import org.joda.time.DateTime

case class FileInsertionResult(fileId: String, createdAt: DateTime, path: String, size: Long)
