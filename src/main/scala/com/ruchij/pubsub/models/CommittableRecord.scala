package com.ruchij.pubsub.models

case class CommittableRecord[F[_], A](data: A, commit: F[Unit])