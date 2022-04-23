package com.ruchij.web.requests.ws

import com.ruchij.services.messages.models.UserMessage
import com.ruchij.services.messages.models.UserMessage.{Group, OneToOne}

case class OutboundMessage(messageType: MessageType, message: UserMessage)

object OutboundMessage {
  def messageType(userMessage: UserMessage): MessageType =
    userMessage match {
      case _: OneToOne => MessageType.OneToOne
      case _: Group => MessageType.GroupMessage
    }

  def fromUserMessage(userMessage: UserMessage): OutboundMessage =
    OutboundMessage(messageType(userMessage), userMessage)

}
