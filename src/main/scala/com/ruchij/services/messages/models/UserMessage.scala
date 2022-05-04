package com.ruchij.services.messages.models

import org.joda.time.DateTime

sealed trait UserMessage {
  val messageId: String
  val senderId: String
  val sentAt: DateTime
  val message: String
}

object UserMessage {
  case class OneToOne(messageId: String, senderId: String, sentAt: DateTime, receiverId: String, message: String)
      extends UserMessage

  case class Group(messageId: String, senderId: String, sentAt: DateTime, groupId: String, message: String)
      extends UserMessage
}
