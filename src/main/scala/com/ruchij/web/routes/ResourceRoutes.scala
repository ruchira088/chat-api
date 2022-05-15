package com.ruchij.web.routes

import cats.data.OptionT
import cats.effect.kernel.Sync
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, StaticFile}

object ResourceRoutes {
  private val ServedFiles = Set("index.html", "script.js", "style.css")

  def apply[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes {
      case request @ GET -> Root =>
        StaticFile.fromResource("index.html", Some(request))

      case request @ GET -> Root / fileName if ServedFiles.contains(fileName) =>
        StaticFile.fromResource(fileName, Some(request))

      case _ => OptionT.none
    }
  }

}
