package com.ruchij.web.routes

import cats.data.OptionT
import cats.effect.kernel.Sync
import org.http4s.Uri.Path.Segment
import org.http4s.{HttpRoutes, StaticFile}
import org.http4s.dsl.Http4sDsl

object ResourceRoutes {

  def apply[F[_]: Sync](implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    HttpRoutes {
      case request @ GET -> Root =>
        StaticFile.fromResource("http/index.html", Some(request))

      case request @ GET -> "resources" /: path =>
        StaticFile.fromResource(Path(Vector(Segment("http"))).concat(path).renderString, Some(request))

      case _ => OptionT.none
    }
  }

}
