package com.ruchij.web.routes

import cats.data.OptionT
import cats.implicits._
import cats.effect.kernel.Sync
import com.ruchij.dao.user.models.User
import com.ruchij.services.authentication.AuthenticationService
import com.ruchij.services.filestore.FileStore
import com.ruchij.web.middleware.UserAuthenticator
import org.http4s.{ContextRoutes, Headers, HttpRoutes, Response, Status}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{`Content-Length`, `Content-Type`}
import com.ruchij.types.FunctionKTypes._

object FileResourceRoutes {
  def apply[F[_]: Sync](fileStore: FileStore[F], authenticationService: AuthenticationService[F])(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    UserAuthenticator(authenticationService).apply {
      ContextRoutes[User, F] {
        case GET -> Root / "id" / fileId as _ =>
          OptionT(fileStore.retrieve(fileId))
            .semiflatMap { fileResource =>
              `Content-Length`.fromLong(fileResource.size).toType[F, Throwable]
                .map { contentLength =>
                  Response(
                    status = Status.Ok,
                    headers = Headers(`Content-Type`(fileResource.mediaType), contentLength),
                    body = fileResource.data
                  )
                }
            }

        case _ => OptionT.none
      }
    }
  }
}
