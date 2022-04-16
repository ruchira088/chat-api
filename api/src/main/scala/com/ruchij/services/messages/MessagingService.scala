package com.ruchij.services.messages

import com.ruchij.dao.user.models.User
import com.ruchij.services.messages.models.Message
import fs2.concurrent.Channel

trait MessagingService[F[_]] {
  def register(user: User, channel: Channel[F, Message]): F[Unit]

  def unregister(user: User): F[Unit]
}
