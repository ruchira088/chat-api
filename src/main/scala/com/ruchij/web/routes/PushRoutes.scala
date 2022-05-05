package com.ruchij.web.routes

import cats.effect.Async
import cats.implicits._
import com.ruchij.circe.Decoders.dateTimeDecoder
import com.ruchij.services.messages.MessagingService
import com.ruchij.services.messages.models.UserMessage
import com.ruchij.services.messages.models.UserMessage.userMessageDecoder
import com.ruchij.web.middleware.ServiceAuthenticator
import com.ruchij.web.requests.RequestOps
import com.ruchij.web.responses.PushResponse
import com.ruchij.web.validate.Validator.baseValidator
import io.circe.generic.auto.{exportDecoder, exportEncoder}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl

object PushRoutes {
  def apply[F[_]: Async](messagingService: MessagingService[F], serviceToken: String)(implicit dsl: Http4sDsl[F]): HttpRoutes[F] = {
    import dsl._

    ServiceAuthenticator[F](serviceToken).apply {
      HttpRoutes.of {
        case request @ POST -> Root =>
          for {
            userMessage <- request.to[UserMessage]
            success <- messagingService.sendToUser(userMessage.senderId, userMessage)
            response <- if (success) Accepted(PushResponse(true)) else NotAcceptable(PushResponse(false))
          }
          yield response
      }
    }
  }

}
