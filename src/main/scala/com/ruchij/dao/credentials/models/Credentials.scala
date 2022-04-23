package com.ruchij.dao.credentials.models

import org.joda.time.DateTime

case class Credentials(
  userId: String,
  createdAt: DateTime,
  lastUpdatedAt: DateTime,
  saltedHashedPassword: SaltedHashValue
)
