package com.ruchij.services.authentication.models

import org.joda.time.DateTime

case class AuthenticationTokenDetails(
  userId: String,
  authenticationToken: AuthenticationToken,
  issuedAt: DateTime,
  expiresAt: DateTime,
  renewals: Long
)
