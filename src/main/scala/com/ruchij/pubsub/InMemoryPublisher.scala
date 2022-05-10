package com.ruchij.pubsub

import cats.effect.kernel.Sync
import com.ruchij.services.messages.models.Message
import com.ruchij.types.Logger
import fs2.Pipe

class InMemoryPublisher[F[_]: Sync] extends Publisher[F, Message] {
  private val logger = Logger[InMemoryPublisher[F]]

  override val publish: Pipe[F, Message, Unit] =
    input => input.evalMap(userMessage => logger.info(userMessage.toString))

}
