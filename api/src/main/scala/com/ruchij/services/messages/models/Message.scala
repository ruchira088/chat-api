package com.ruchij.services.messages.models

import com.ruchij.services.authentication.models.AuthenticationToken

sealed trait Message

object Message {
  sealed trait ServiceMessage extends Message

  case class Authentication(authenticationToken: AuthenticationToken) extends ServiceMessage

  sealed trait UserMessage extends Message {
    val message: String
  }

  case class PersonalMessage(receiverUserId: String, message: String) extends UserMessage
  case class GroupMessage(groupId: String, message: String) extends UserMessage

}
