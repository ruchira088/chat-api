package com.ruchij.services.messages

import cats.data.OptionT
import cats.effect.kernel.Sync
import cats.implicits._
import com.ruchij.dao.user.models.User
import com.ruchij.pub.Publisher
import com.ruchij.services.messages.models.UserMessage
import fs2.Pipe
import fs2.concurrent.Channel

import java.util.concurrent.ConcurrentHashMap

class MessagingServiceImpl[F[_]: Sync](publisher: Publisher[F, UserMessage]) extends MessagingService[F] {

  private val userIdToChannelMappings = new ConcurrentHashMap[String, Channel[F, UserMessage]]()

  override def register(user: User, channel: Channel[F, UserMessage]): F[Unit] =
    Sync[F].delay(userIdToChannelMappings.put(user.id, channel)).as((): Unit)

  override def unregister(user: User): F[Unit] =
    Sync[F].delay(userIdToChannelMappings.remove(user.id)).as((): Unit)

  override def sendToUser(userId: String, userMessage: UserMessage): F[Boolean] =
    OptionT(Sync[F].delay(Option(userIdToChannelMappings.get(userId))))
      .semiflatMap { channel => channel.send(userMessage) }
      .map { result => result.fold(_ => false, _ => true) }
      .getOrElse(false)

  override val submit: Pipe[F, UserMessage, Unit] = publisher.publish
}
