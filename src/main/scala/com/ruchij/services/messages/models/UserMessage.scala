package com.ruchij.services.messages.models

import org.joda.time.DateTime

sealed trait UserMessage {
  val senderId: String
}

object UserMessage {
  case class OneToOne(senderId: String, sentAt: DateTime, receiverId: String, message: String) extends UserMessage
  case class Group(senderId: String, groupId: String, message: String) extends UserMessage
}
