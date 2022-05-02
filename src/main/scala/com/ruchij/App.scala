package com.ruchij

import cats.data.Reader
import cats.effect.kernel.Resource
import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.~>
import com.ruchij.config.ServiceConfiguration
import com.ruchij.dao.credentials.DoobieCredentialsDao
import com.ruchij.dao.doobie.DoobieTransactor
import com.ruchij.dao.user.DoobieUserDao
import com.ruchij.kv.{KeySpace, KeySpacedKeyValueStore, RedisKeyValueStore}
import com.ruchij.migration.MigrationApp
import com.ruchij.pubsub.InMemoryPublisher
import com.ruchij.pubsub.kafka.KafkaPublisher
import com.ruchij.services.authentication.AuthenticationServiceImpl
import com.ruchij.services.authentication.models.{AuthenticationToken, AuthenticationTokenDetails}
import com.ruchij.services.hashing.BcryptPasswordHashingService
import com.ruchij.services.health.HealthServiceImpl
import com.ruchij.services.messages.MessagingServiceImpl
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.types.JodaClock
import com.ruchij.web.Routes
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.websocket.WebSocketBuilder2
import pureconfig.ConfigSource

object App extends IOApp {
  case class ApplicationResources[F[_]](
    redisCommands: RedisCommands[F, String, String],
    kafkaProducer: KafkaProducer[String, SpecificRecord],
    hikariTransactor: HikariTransactor[F]
  )

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      serviceConfiguration <- ServiceConfiguration.parse[IO](configObjectSource)

      _ <- MigrationApp.migrate[IO](serviceConfiguration.databaseConfiguration)

      _ <- createApplicationResources[IO](serviceConfiguration).use { applicationResources =>
        BlazeServerBuilder[IO]
          .withHttpWebSocketApp(httpApplication(applicationResources, serviceConfiguration).run)
          .bindHttp(serviceConfiguration.httpConfiguration.port, serviceConfiguration.httpConfiguration.host)
          .serve
          .compile
          .drain
      }

    } yield ExitCode.Success

  def createApplicationResources[F[_]: Async](
    serviceConfiguration: ServiceConfiguration
  ): Resource[F, ApplicationResources[F]] =
    for {
      redisCommands <- Redis[F].utf8(serviceConfiguration.redisConfiguration.uri)
      kafkaProducer <- KafkaPublisher.createProducer[F](serviceConfiguration.kafkaConfiguration)
      hikariTransactor <- DoobieTransactor.create(serviceConfiguration.databaseConfiguration)
    } yield ApplicationResources(redisCommands, kafkaProducer, hikariTransactor)

  def httpApplication[F[_]: Async: JodaClock](
    applicationResources: ApplicationResources[F],
    serviceConfiguration: ServiceConfiguration
  ): Reader[WebSocketBuilder2[F], HttpApp[F]] = {
    implicit val transaction: ConnectionIO ~> F = applicationResources.hikariTransactor.trans
    val passwordHashingService = new BcryptPasswordHashingService[F]
    val keyValueStore = new RedisKeyValueStore[F](applicationResources.redisCommands)

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

    val messagingService = new MessagingServiceImpl[F](new InMemoryPublisher[F], serviceConfiguration.instanceConfiguration)

    val healthService = new HealthServiceImpl[F](serviceConfiguration.buildInformation)

    Reader[WebSocketBuilder2[F], HttpApp[F]] { webSocketBuilder =>
      Routes[F](userService, authenticationService, messagingService, healthService, webSocketBuilder)
    }
  }
}
