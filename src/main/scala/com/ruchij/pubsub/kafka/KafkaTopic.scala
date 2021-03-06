package com.ruchij.pubsub.kafka

import com.ruchij.avro.chat.OneToOneMessage
import com.ruchij.services.messages.models.Message
import com.ruchij.services.messages.models.Message.OneToOne
import org.apache.avro.specific.SpecificRecord
import org.joda.time.DateTime

import java.time.Instant

trait KafkaTopic[A <: Message, B <: SpecificRecord] {
  val name: String

  def toSpecificRecord(value: A): B

  def fromSpecificRecord(record: B): A

  def key(value: A): String
}

object KafkaTopic {
  object OneToOneMessageTopic extends KafkaTopic[OneToOne, OneToOneMessage] {
    override val name: String = "one-to-one"

    override def toSpecificRecord(oneToOne: OneToOne): OneToOneMessage =
      OneToOneMessage
        .newBuilder()
        .setMessageId(oneToOne.messageId)
        .setSenderId(oneToOne.senderId)
        .setReceiverId(oneToOne.receiverId)
        .setMessage(oneToOne.content)
        .setSentAt(Instant.ofEpochMilli(oneToOne.sentAt.getMillis))
        .build()

    override def key(oneToOne: OneToOne): String = oneToOne.receiverId

    override def fromSpecificRecord(record: OneToOneMessage): OneToOne =
      OneToOne(
        record.getMessageId.toString,
        record.getSenderId.toString,
        new DateTime(record.getSentAt.toEpochMilli),
        record.getReceiverId.toString,
        record.getMessage.toString
      )
  }

}
