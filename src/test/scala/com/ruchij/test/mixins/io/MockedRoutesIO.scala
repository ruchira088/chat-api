package com.ruchij.test.mixins.io

import cats.effect.{Async, IO}
import com.ruchij.test.mixins.MockedRoutes
import com.ruchij.types.JodaClock

trait MockedRoutesIO extends MockedRoutes[IO] {
  override val async: Async[IO] = IO.asyncForIO

  override val jodaClock: JodaClock[IO] = JodaClock.create[IO]
}
