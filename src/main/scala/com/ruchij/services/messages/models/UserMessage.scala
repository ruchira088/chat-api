package com.ruchij.services.messages.models

import io.circe.{Decoder, Encoder}
import org.joda.time.DateTime

sealed trait UserMessage extends Message {
  val senderId: String
  val sentAt: DateTime
  val message: String
}

object UserMessage {
  case class OneToOne(messageId: String, senderId: String, sentAt: DateTime, receiverId: String, message: String)
      extends UserMessage

  case class Group(messageId: String, senderId: String, sentAt: DateTime, groupId: String, message: String)
      extends UserMessage

  implicit def userMessageDecoder(
    implicit oneToOneDecoder: Decoder[OneToOne],
    groupDecoder: Decoder[Group]
  ): Decoder[UserMessage] =
    oneToOneDecoder.map(identity[UserMessage]).or { groupDecoder.map(identity[UserMessage]) }

  implicit def userMessageEncoder(
    implicit oneToOneEncoder: Encoder[OneToOne],
    groupEncoder: Encoder[Group]
  ): Encoder[UserMessage] = {
    case oneToOne: OneToOne => oneToOneEncoder(oneToOne)
    case group: Group => groupEncoder(group)
  }
}
