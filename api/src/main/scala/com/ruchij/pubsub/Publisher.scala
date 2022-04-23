package com.ruchij.pubsub

import fs2.Pipe

trait Publisher[F[_], A]
{
  val publish: Pipe[F, A, Unit]
}