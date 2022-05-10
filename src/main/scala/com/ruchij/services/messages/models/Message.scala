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

  case class HeartBeat(messageId: String, sentAt: DateTime) extends Message

  implicit def messageCirceDecoder(
    implicit oneToOneDecoder: Decoder[OneToOne],
    groupDecoder: Decoder[Group],
    heartBeatDecoder: Decoder[HeartBeat]
  ): Decoder[Message] =
    oneToOneDecoder.map(identity[Message])
      .or(groupDecoder.map(identity[Message]))
      .or(heartBeatDecoder.map(identity[Message]))

  implicit def messageCirceEncoder(
    implicit oneToOneEncoder: Encoder[OneToOne],
    groupEncoder: Encoder[Group],
    heartBeatEncoder: Encoder[HeartBeat]
  ): Encoder[Message] = {
    case oneToOne: OneToOne => oneToOneEncoder(oneToOne)
    case group: Group => groupEncoder(group)
    case heartBeat: HeartBeat => heartBeatEncoder(heartBeat)
  }
}
