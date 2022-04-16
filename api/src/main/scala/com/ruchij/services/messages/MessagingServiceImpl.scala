package com.ruchij.services.messages
import com.ruchij.dao.user.models.User
import com.ruchij.services.messages.models.Message
import fs2.concurrent.Channel

class MessagingServiceImpl[F[_]] extends MessagingService[F] {

  override def register(user: User, channel: Channel[F, Message]): F[Unit] = ???

  override def unregister(user: User): F[Unit] = ???

}
