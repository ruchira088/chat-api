package com.ruchij.web.requests

import cats.effect.kernel.Concurrent
import fs2.Stream
import org.http4s.headers.`Content-Type`
import org.http4s.{DecodeResult, EntityDecoder, InvalidMessageBodyFailure, MediaType}

case class ProfileImageRequest[F[_]](fileName: String, mediaType: MediaType, data: Stream[F, Byte])

object ProfileImageRequest {

  implicit def profileImageRequestDecoder[F[_]: Concurrent]: EntityDecoder[F, ProfileImageRequest[F]] =
    EntityDecoder.multipart[F].flatMapR { multipart =>
      multipart.parts
        .find(part => part.filename.nonEmpty && part.name.contains("image"))
        .flatMap { part =>
          part.filename.flatMap { filename =>
            part.headers.get[`Content-Type`].map { contentType =>
              ProfileImageRequest(filename, contentType.mediaType, part.body)
            }
          }
        }
        .fold(
          DecodeResult
            .failureT[F, ProfileImageRequest[F]](InvalidMessageBodyFailure("Unable to find file in image field"))
        ) { profileImageRequest =>
          DecodeResult.successT[F, ProfileImageRequest[F]](profileImageRequest)
        }
    }

}
