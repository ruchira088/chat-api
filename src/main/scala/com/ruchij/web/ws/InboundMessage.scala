package com.ruchij.web.ws

import com.ruchij.services.authentication.models.AuthenticationToken
import com.ruchij.services.messages.models.UserMessage
import io.circe.generic.auto.exportDecoder
import io.circe.{Decoder, HCursor, Json}
import org.joda.time.DateTime

sealed trait InboundMessage

object InboundMessage {
  case class Authentication(authenticationToken: AuthenticationToken) extends InboundMessage
  case class SendOneToOneMessageInbound(receiverId: String, message: String) extends InboundMessage
  case class SendGroupMessageInbound(groupId: String, message: String) extends InboundMessage

  private case class TypedWebSocketMessage(messageType: MessageType, message: Json)

  implicit val inboundWebSocketMessageDecoder: Decoder[InboundMessage] =
    (cursor: HCursor) =>
      cursor.as[TypedWebSocketMessage]
        .flatMap { typedWebSocketMessage =>
          val decoder =
            typedWebSocketMessage.messageType match {
              case MessageType.Authentication => Decoder[Authentication]
              case MessageType.OneToOne => Decoder[SendOneToOneMessageInbound]
              case MessageType.GroupMessage => Decoder[SendGroupMessageInbound]
            }

          decoder.decodeJson(typedWebSocketMessage.message)
        }

  def toUserMessage(userId: String, webSocketMessage: InboundMessage, timestamp: DateTime): Option[UserMessage] =
    webSocketMessage match {
      case SendOneToOneMessageInbound(receiverId, message) => Some(UserMessage.OneToOne(userId, timestamp, receiverId, message))
      case SendGroupMessageInbound(groupId, message) => Some(UserMessage.Group(userId, groupId, message))
      case _ => None
    }

}
