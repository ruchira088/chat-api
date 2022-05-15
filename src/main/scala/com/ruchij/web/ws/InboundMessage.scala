package com.ruchij.web.ws

import com.ruchij.circe.Decoders.enumDecoder
import com.ruchij.services.messages.models.Message
import io.circe.generic.auto.exportDecoder
import io.circe.{Decoder, DecodingFailure, HCursor, Json}
import org.joda.time.DateTime

sealed trait InboundMessage {
  val messageId: String
}

object InboundMessage {
  case class SendOneToOneMessageInbound(messageId: String, receiverId: String, content: String) extends InboundMessage
  case class SendGroupMessageInbound(messageId: String, groupId: String, content: String) extends InboundMessage

  private case class TypedWebSocketMessage(messageType: MessageType, message: Json)

  implicit val inboundWebSocketMessageDecoder: Decoder[InboundMessage] =
    (cursor: HCursor) =>
      cursor.as[TypedWebSocketMessage]
        .flatMap { typedWebSocketMessage =>
          val decoder: PartialFunction[MessageType, Decoder[InboundMessage]] = {
              case MessageType.OneToOne => Decoder[SendOneToOneMessageInbound].map(identity[InboundMessage])
              case MessageType.GroupMessage => Decoder[SendGroupMessageInbound].map(identity[InboundMessage])
          }

          decoder.andThen(_.decodeJson(typedWebSocketMessage.message))
            .applyOrElse[MessageType,Decoder.Result[InboundMessage]](
              typedWebSocketMessage.messageType,
              messageType => Left(DecodingFailure(s"$messageType is NOT supported", cursor.history))
            )
        }

  def toMessage(userId: String, webSocketMessage: InboundMessage, timestamp: DateTime): Option[Message] =
    webSocketMessage match {
      case SendOneToOneMessageInbound(messageId, receiverId, message) =>
        Some(Message.OneToOne(messageId, userId, timestamp, receiverId, message))

      case SendGroupMessageInbound(messageId, groupId, message) =>
        Some(Message.Group(messageId, userId, timestamp, groupId, message))

      case _ => None
    }

}
