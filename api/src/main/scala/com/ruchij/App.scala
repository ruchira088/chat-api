package com.ruchij

import cats.effect.kernel.Resource
import cats.effect.{Async, ExitCode, IO, IOApp}
import com.ruchij.circe.Decoders.{authenticationTokenDecoder, dateTimeDecoder}
import com.ruchij.circe.Encoders.{authenticationTokenEncoder, dateTimeEncoder}
import com.ruchij.config.ServiceConfiguration
import com.ruchij.dao.credentials.DoobieCredentialsDao
import com.ruchij.dao.doobie.DoobieTransactor
import com.ruchij.dao.user.DoobieUserDao
import com.ruchij.kv.{KeySpace, KeySpacedKeyValueStore, RedisKeyValueStore}
import com.ruchij.migration.MigrationApp
import com.ruchij.services.authentication.AuthenticationServiceImpl
import com.ruchij.services.authentication.models.{AuthenticationToken, AuthenticationTokenDetails}
import com.ruchij.services.hashing.BcryptPasswordHashingService
import com.ruchij.services.health.HealthServiceImpl
import com.ruchij.services.messages.MessagingServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.web.Routes
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import dev.profunktor.redis4cats.Redis
import doobie.ConnectionIO
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.websocket.WebSocketBuilder2
import io.circe.generic.auto._
import pureconfig.ConfigSource

object App extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      serviceConfiguration <- ServiceConfiguration.parse[IO](configObjectSource)

      _ <- MigrationApp.migrate[IO](serviceConfiguration.databaseConfiguration)

      _ <- httpApplication[IO](serviceConfiguration).use { httpApp =>
        BlazeServerBuilder[IO]
          .withHttpWebSocketApp(httpApp)
          .bindHttp(serviceConfiguration.httpConfiguration.port, serviceConfiguration.httpConfiguration.host)
          .serve
          .compile
          .drain
      }

    } yield ExitCode.Success

  def httpApplication[F[_]: Async](
    serviceConfiguration: ServiceConfiguration
  ): Resource[F, WebSocketBuilder2[F] => HttpApp[F]] =
    Redis[F].utf8(serviceConfiguration.redisConfiguration.uri).flatMap { redisCommands =>
      DoobieTransactor
        .create(serviceConfiguration.databaseConfiguration)
        .map(_.trans)
        .map { implicit transaction =>
          val passwordHashingService = new BcryptPasswordHashingService[F]
          val keyValueStore = new RedisKeyValueStore[F](redisCommands)

          val authenticationKeyValueStore =
            new KeySpacedKeyValueStore[F, AuthenticationToken, AuthenticationTokenDetails](
              keyValueStore,
              KeySpace.AuthenticationKeySpace
            )

          val authenticationService = new AuthenticationServiceImpl[F, ConnectionIO](
            authenticationKeyValueStore,
            passwordHashingService,
            DoobieUserDao,
            DoobieCredentialsDao
          )

          val userService =
            new UserServiceImpl[F, ConnectionIO](passwordHashingService, DoobieUserDao, DoobieCredentialsDao)

          val messagingService = new MessagingServiceImpl[F]

          val healthService = new HealthServiceImpl[F](serviceConfiguration.buildInformation)

          webSocketBuilder =>
            Routes[F](userService, authenticationService, messagingService, healthService, webSocketBuilder)
        }

    }
}
