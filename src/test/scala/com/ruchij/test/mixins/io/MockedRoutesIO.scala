package com.ruchij.test.mixins.io

import cats.effect.{Async, IO}
import com.ruchij.test.mixins.MockedRoutes

trait MockedRoutesIO extends MockedRoutes[IO] {
  override val async: Async[IO] = IO.asyncForIO
}
