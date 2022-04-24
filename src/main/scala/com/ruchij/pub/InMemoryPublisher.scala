package com.ruchij.pub

import cats.effect.kernel.Sync
import com.ruchij.services.messages.models.UserMessage
import com.ruchij.types.Logger
import fs2.Pipe

class InMemoryPublisher[F[_]: Sync] extends Publisher[F, UserMessage] {
  private val logger = Logger[InMemoryPublisher[F]]

  override val publish: Pipe[F, UserMessage, Unit] =
    input => input.evalMap(userMessage => logger.info(userMessage.toString))

}
