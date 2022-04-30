package com.ruchij.pubsub

import com.ruchij.pubsub.models.CommittableRecord
import fs2.Stream

trait Subscriber[F[_], A] {
  def subscribe(groupId: String): Stream[F, CommittableRecord[F, A]]
}
