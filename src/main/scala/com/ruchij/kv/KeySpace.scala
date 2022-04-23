package com.ruchij.kv

import com.ruchij.services.authentication.models.{AuthenticationToken, AuthenticationTokenDetails}

trait KeySpace[K, V] {
  val name: String
}

object KeySpace {
  case object AuthenticationKeySpace extends KeySpace[AuthenticationToken, AuthenticationTokenDetails] {
    override val name: String = "authentication"
  }
}
