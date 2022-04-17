package com.ruchij.services.messages

import cats.data.OptionT
import cats.effect.kernel.Sync
import cats.implicits._
import com.ruchij.dao.user.models.User
import com.ruchij.services.messages.models.Message
import fs2.concurrent.Channel

import java.util.concurrent.ConcurrentHashMap

class MessagingServiceImpl[F[_]: Sync] extends MessagingService[F] {

  private val userIdToChannelMappings = new ConcurrentHashMap[String, Channel[F, Message]]()

  override def register(user: User, channel: Channel[F, Message]): F[Unit] =
    Sync[F].delay(userIdToChannelMappings.put(user.id, channel)).as((): Unit)

  override def unregister(user: User): F[Unit] =
    Sync[F].delay(userIdToChannelMappings.remove(user.id)).as((): Unit)

  override def submit(user: User, message: Message): F[Unit] = ???

  override def sendToUser(userId: String, message: Message): F[Boolean] =
    OptionT(Sync[F].delay(Option(userIdToChannelMappings.get(userId))))
      .semiflatMap { channel => channel.send(message) }
      .map { result => result.fold(_ => false, _ => true) }
      .getOrElse(false)
}
