package com.ruchij

import cats.data.Reader
import cats.effect.kernel.Resource
import cats.effect.{Async, ExitCode, IO, IOApp}
import cats.implicits._
import cats.~>
import com.ruchij.avro.chat.OneToOneMessage
import com.ruchij.config.{InstanceConfiguration, ServiceConfiguration}
import com.ruchij.dao.credentials.DoobieCredentialsDao
import com.ruchij.dao.doobie.DoobieTransactor
import com.ruchij.dao.file.MongoFileMetadataDao
import com.ruchij.dao.mongo.MongoClient
import com.ruchij.dao.profile.DoobieProfileImageDao
import com.ruchij.dao.user.DoobieUserDao
import com.ruchij.kv.{KeySpace, KeySpacedKeyValueStore, RedisKeyValueStore}
import com.ruchij.migration.MigrationApp
import com.ruchij.pubsub.Publisher
import com.ruchij.pubsub.kafka.{KafkaPublisher, KafkaTopic}
import com.ruchij.services.authentication.AuthenticationServiceImpl
import com.ruchij.services.authentication.models.{AuthenticationToken, AuthenticationTokenDetails}
import com.ruchij.services.filestore.LocalFileStore
import com.ruchij.services.hashing.BcryptPasswordHashingService
import com.ruchij.services.health.HealthServiceImpl
import com.ruchij.services.messages.MessagingServiceImpl
import com.ruchij.services.messages.models.Message
import com.ruchij.services.messages.models.Message.OneToOne
import com.ruchij.services.user.UserServiceImpl
import com.ruchij.types.FunctionKTypes.{WrappedFuture, identityFunctionK, ioFutureToIO}
import com.ruchij.types.JodaClock
import com.ruchij.types.RandomGenerator.IdGenerator
import com.ruchij.web.Routes
import dev.profunktor.redis4cats.effect.Log.Stdout.instance
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import fs2.{Pipe, Stream}
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.http4s.HttpApp
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.websocket.WebSocketBuilder2
import org.mongodb.scala.MongoDatabase
import pureconfig.ConfigSource

object ApiApp extends IOApp {
  case class ApplicationResources[F[_]](
    httpClient: Client[F],
    redisCommands: RedisCommands[F, String, String],
    kafkaProducer: KafkaProducer[String, SpecificRecord],
    hikariTransactor: HikariTransactor[F],
    mongoDatabase: MongoDatabase
  )

  override def run(args: List[String]): IO[ExitCode] =
    for {
      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      serviceConfiguration <- ServiceConfiguration.parse[IO](configObjectSource)
      exitCode <- run[IO](serviceConfiguration)
    } yield exitCode

  def run[F[_]: Async: JodaClock](
    serviceConfiguration: ServiceConfiguration
  )(implicit futureUnwrapper: WrappedFuture[F, *] ~> F): F[ExitCode] =
    MigrationApp
      .migrate[F](serviceConfiguration.databaseConfiguration)
      .productR {
        createApplicationResources[F](serviceConfiguration).use { applicationResources =>
          EmberServerBuilder.default[F]
            .withHttpWebSocketApp(httpApplication(applicationResources, serviceConfiguration).run)
            .withHost(serviceConfiguration.httpConfiguration.host)
            .withPort(serviceConfiguration.httpConfiguration.port)
            .build
            .use(_ => Async[F].never.as(ExitCode.Success))
        }
      }

  def createApplicationResources[F[_]: Async](
    serviceConfiguration: ServiceConfiguration
  ): Resource[F, ApplicationResources[F]] =
    for {
      client <- EmberClientBuilder.default[F].build
      redisCommands <- Redis[F].utf8(serviceConfiguration.redisConfiguration.uri)
      kafkaProducer <- KafkaPublisher.createProducer[F](serviceConfiguration.kafkaConfiguration)
      hikariTransactor <- DoobieTransactor.create(serviceConfiguration.databaseConfiguration)
      mongoDatabase <- MongoClient.create(serviceConfiguration.mongoConfiguration)

    } yield ApplicationResources(client, redisCommands, kafkaProducer, hikariTransactor, mongoDatabase)

  def httpApplication[F[_]: Async: JodaClock: IdGenerator](
    applicationResources: ApplicationResources[F],
    serviceConfiguration: ServiceConfiguration
  )(implicit futureUnwrapper: WrappedFuture[F, *] ~> F): Reader[WebSocketBuilder2[F], HttpApp[F]] = {
    implicit val transaction: ConnectionIO ~> F = applicationResources.hikariTransactor.trans
    val passwordHashingService = new BcryptPasswordHashingService[F]
    val keyValueStore = new RedisKeyValueStore[F](applicationResources.redisCommands)

    val authenticationKeyValueStore =
      new KeySpacedKeyValueStore[F, AuthenticationToken, AuthenticationTokenDetails](
        keyValueStore,
        KeySpace.AuthenticationKeySpace
      )

    val userSessionKeyValueStore =
      new KeySpacedKeyValueStore[F, String, InstanceConfiguration](keyValueStore, KeySpace.UserSessionKeySpace)

    val authenticationService =
      new AuthenticationServiceImpl[F, ConnectionIO](
        authenticationKeyValueStore,
        passwordHashingService,
        DoobieUserDao,
        DoobieCredentialsDao
      )

    val mongoFileMetadataDao = new MongoFileMetadataDao[F](applicationResources.mongoDatabase)

    val fileStore = new LocalFileStore[F, F](serviceConfiguration.fileStoreConfiguration, mongoFileMetadataDao)

    val userService =
      new UserServiceImpl[F, ConnectionIO](
        passwordHashingService,
        fileStore,
        DoobieUserDao,
        DoobieCredentialsDao,
        DoobieProfileImageDao
      )

    val messagingService =
      new MessagingServiceImpl[F](
        kafkaPublisher(applicationResources.kafkaProducer),
        userSessionKeyValueStore,
        applicationResources.httpClient,
        serviceConfiguration.instanceConfiguration,
        serviceConfiguration.authenticationConfiguration.serviceAuthentication
      )

    val healthService = new HealthServiceImpl[F](serviceConfiguration.buildInformation)

    Reader[WebSocketBuilder2[F], HttpApp[F]] { webSocketBuilder =>
      Routes[F](
        userService,
        authenticationService,
        messagingService,
        healthService,
        webSocketBuilder,
        fileStore,
        serviceConfiguration.authenticationConfiguration.serviceAuthentication
      )
    }
  }

  def kafkaPublisher[F[_]: Async](
    kafkaProducer: KafkaProducer[String, SpecificRecord]
  )(implicit futureUnwrapper: WrappedFuture[F, *] ~> F): Publisher[F, Message] = {
    val oneToOnePublisher =
      new KafkaPublisher[F, OneToOne, OneToOneMessage](kafkaProducer, KafkaTopic.OneToOneMessageTopic)

    new Publisher[F, Message] {
      override val publish: Pipe[F, Message, Unit] =
        input =>
          input.flatMap {
            case oneToOne: OneToOne => oneToOnePublisher.publish(Stream.emit[F, OneToOne](oneToOne))
            case message =>
              Stream.raiseError[F] {
                new NotImplementedError(s"${message.getClass.getSimpleName} messages are NOT supported")
              }
        }
    }
  }
}
