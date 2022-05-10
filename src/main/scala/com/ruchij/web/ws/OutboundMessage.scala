package com.ruchij.web.ws

import com.ruchij.services.messages.models.Message
import com.ruchij.services.messages.models.Message.{Group, OneToOne}

case class OutboundMessage(messageType: MessageType, message: Message)

object OutboundMessage {
  def messageType(message: Message): MessageType =
    message match {
      case _: OneToOne => MessageType.OneToOne
      case _: Group => MessageType.GroupMessage
    }

  def fromMessage(message: Message): OutboundMessage =
    OutboundMessage(messageType(message), message)

}
