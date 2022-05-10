package com.ruchij.services.messages

import cats.data.OptionT
import cats.effect.kernel.Sync
import cats.implicits._
import com.ruchij.config.AuthenticationConfiguration.ServiceAuthenticationConfiguration
import com.ruchij.config.InstanceConfiguration
import com.ruchij.dao.user.models.User
import com.ruchij.circe.Encoders.dateTimeEncoder
import com.ruchij.kv.KeySpacedKeyValueStore
import com.ruchij.pubsub.Publisher
import com.ruchij.services.messages.models.Message
import com.ruchij.services.messages.models.Message.messageCirceEncoder
import com.ruchij.types.FunctionKTypes.{FunctionK2TypeOps, eitherToF}
import com.ruchij.web.requests.UserPushMessageRequest
import fs2.Pipe
import fs2.concurrent.Channel
import io.circe.generic.auto.exportEncoder
import org.http4s.Method.POST
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Authorization
import org.http4s.{Credentials, Uri}
import org.typelevel.ci.CIStringSyntax

import java.util.concurrent.ConcurrentHashMap

class MessagingServiceImpl[F[_]: Sync](
  publisher: Publisher[F, Message],
  keySpacedKeyValueStore: KeySpacedKeyValueStore[F, String, InstanceConfiguration],
  client: Client[F],
  instanceConfiguration: InstanceConfiguration,
  serviceAuthenticationConfiguration: ServiceAuthenticationConfiguration
) extends MessagingService[F] {
  private val clientDsl: Http4sClientDsl[F] = Http4sClientDsl[F]
  private val userIdToChannelMappings = new ConcurrentHashMap[String, Channel[F, Message]]()

  import clientDsl._

  override def register(user: User, channel: Channel[F, Message]): F[Unit] =
    Sync[F]
      .delay(userIdToChannelMappings.put(user.id, channel))
      .productR(keySpacedKeyValueStore.insert(user.id, instanceConfiguration))
      .as((): Unit)

  override def unregister(user: User): F[Unit] =
    Sync[F].delay(userIdToChannelMappings.remove(user.id)).as((): Unit)

  override def sendToUser(userId: String, message: Message): F[Boolean] =
    OptionT(Sync[F].delay(Option(userIdToChannelMappings.get(userId))))
      .semiflatMap { channel =>
        channel.send(message)
      }
      .map { result =>
        result.fold(_ => false, _ => true)
      }
      .getOrElseF(sendToRemoteUser(userId, message))

  private def sendToRemoteUser(userId: String, message: Message): F[Boolean] =
    OptionT(keySpacedKeyValueStore.find(userId))
      .semiflatMap { remoteInstanceConfig =>
        Uri
          .fromString(s"http://${remoteInstanceConfig.hostname}:${remoteInstanceConfig.port}/push")
          .toType[F, Throwable]
      }
      .semiflatMap { uri =>
        client.successful {
          POST(
            UserPushMessageRequest(userId, message),
            uri,
            Authorization(Credentials.Token(ci"Bearer", serviceAuthenticationConfiguration.token))
          )
        }
      }
      .getOrElse(false)

  override val submit: Pipe[F, Message, Unit] = publisher.publish
}
