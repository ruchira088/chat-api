package com.ruchij.services.messages.models

import io.circe.{Decoder, Encoder}
import org.joda.time.DateTime

sealed trait Message {
  val messageId: String
  val sentAt: DateTime
}

object Message {
  case class OneToOne(messageId: String, senderId: String, sentAt: DateTime, receiverId: String, content: String)
      extends Message

  case class Group(messageId: String, senderId: String, sentAt: DateTime, groupId: String, content: String)
      extends Message

  implicit def messageCirceDecoder(
    implicit oneToOneDecoder: Decoder[OneToOne],
    groupDecoder: Decoder[Group]
  ): Decoder[Message] =
    oneToOneDecoder.map(identity[Message])
      .or(groupDecoder.map(identity[Message]))

  implicit def messageCirceEncoder(
    implicit oneToOneEncoder: Encoder[OneToOne],
    groupEncoder: Encoder[Group]
  ): Encoder[Message] = {
    case oneToOne: OneToOne => oneToOneEncoder(oneToOne)
    case group: Group => groupEncoder(group)
  }
}
