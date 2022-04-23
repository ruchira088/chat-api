package com.ruchij.services.messages.models

sealed trait UserMessage {
  val senderId: String
}

object UserMessage {
  case class OneToOne(senderId: String, receiverId: String, message: String) extends UserMessage
  case class Group(senderId: String, groupId: String, message: String) extends UserMessage
}
