package com.ruchij.services.messages

import com.ruchij.dao.user.models.User
import com.ruchij.services.messages.models.UserMessage
import fs2.Pipe
import fs2.concurrent.Channel

trait MessagingService[F[_]] {
  def register(user: User, channel: Channel[F, UserMessage]): F[Unit]

  def unregister(user: User): F[Unit]

  def sendToUser(userId: String, userMessage: UserMessage): F[Boolean]

  val submit: Pipe[F, UserMessage, Unit]
}
