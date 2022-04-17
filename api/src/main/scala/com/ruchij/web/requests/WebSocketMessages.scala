package com.ruchij.web.requests

import com.ruchij.circe.Decoders.authenticationTokenDecoder
import com.ruchij.circe.Encoders.authenticationTokenEncoder
import com.ruchij.services.messages.models.Message
import com.ruchij.services.messages.models.Message.{Authentication, GroupMessage, PersonalMessage}
import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.generic.auto._

object WebSocketMessages {
  sealed trait MessageType extends EnumEntry

  object MessageType extends Enum[MessageType] {
    case object Authentication extends MessageType
    case object PersonalMessage extends MessageType
    case object GroupMessage extends MessageType

    override def values: IndexedSeq[MessageType] = findValues
  }

  private case class TypedMessage(messageType: MessageType)

  private val messageTypeMapper: Message => MessageType = {
    case _: Authentication => MessageType.Authentication
    case _: PersonalMessage => MessageType.PersonalMessage
    case _: GroupMessage => MessageType.GroupMessage
  }

  implicit val messageCirceDecoder: Decoder[Message] =
    (cursor: HCursor) =>
      Decoder[TypedMessage].apply(cursor)
        .map(_.messageType)
        .map {
          case MessageType.Authentication => Decoder[Authentication]
          case MessageType.PersonalMessage => Decoder[PersonalMessage]
          case MessageType.GroupMessage => Decoder[GroupMessage]
        }
        .flatMap {
          decoder => decoder.apply(cursor)
        }

  val messageCirceEncoder: Encoder[Message] = {
    case authentication: Authentication => Encoder[Authentication].apply(authentication)
    case personalMessage: PersonalMessage => Encoder[PersonalMessage].apply(personalMessage)
    case groupMessage: GroupMessage => Encoder[GroupMessage].apply(groupMessage)
  }

  implicit val typedMessageCirceEncoder: Encoder[Message] =
    (message: Message) =>
      messageCirceEncoder.apply(message)
        .deepMerge(Encoder[TypedMessage].apply(TypedMessage(messageTypeMapper(message))))

}
