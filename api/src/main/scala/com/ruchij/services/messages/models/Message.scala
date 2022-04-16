package com.ruchij.services.messages.models

import com.ruchij.services.authentication.models.AuthenticationToken
import io.circe.generic.auto.{exportDecoder, exportEncoder}
import io.circe.{Decoder, Encoder, HCursor}

sealed trait Message

object Message {
  case class Authentication(authenticationToken: AuthenticationToken) extends Message

  private case class TypedMessage(messageType: MessageType)

  private val messageTypeMapper: Message => MessageType = {
    case _: Authentication => MessageType.Authentication
  }

  implicit val messageCirceDecoder: Decoder[Message] =
    (cursor: HCursor) =>
      Decoder[TypedMessage].apply(cursor)
        .map(_.messageType)
        .flatMap {
          case MessageType.Authentication => Decoder[Authentication].apply(cursor)
        }

  val baseMessageCirceEncoder: Encoder[Message] = {
    case authentication: Authentication => Encoder[Authentication].apply(authentication)
  }

  implicit val messageCirceEncoder: Encoder[Message] =
    (message: Message) =>
      baseMessageCirceEncoder.apply(message)
        .deepMerge(Encoder[TypedMessage].apply(TypedMessage(messageTypeMapper(message))))

}
