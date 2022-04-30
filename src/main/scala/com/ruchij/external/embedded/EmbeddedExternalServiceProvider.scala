package com.ruchij.external.embedded

import cats.MonadError
import cats.effect.{Resource, Sync}
import cats.implicits._
import com.ruchij.config.{KafkaConfiguration, RedisConfiguration}
import com.ruchij.external.ExternalServiceProvider
import com.ruchij.external.embedded.EmbeddedExternalServiceProvider.freePort
import com.ruchij.migration.config.DatabaseConfiguration
import com.ruchij.types.RandomGenerator
import io.github.embeddedkafka.EmbeddedKafkaConfig
import io.github.embeddedkafka.schemaregistry.{EmbeddedKafka, EmbeddedKafkaConfig => EmbeddedKafkaSchemaRegistryConfig}
import org.http4s.Uri
import org.http4s.Uri.Scheme
import redis.embedded.RedisServer

import java.net.ServerSocket
import java.util.UUID
import scala.util.Random

class EmbeddedExternalServiceProvider[F[_]: Sync] extends ExternalServiceProvider[F] {

  override val databaseConfiguration: Resource[F, DatabaseConfiguration] =
    Resource.eval(RandomGenerator[F, UUID].generate).map { uuid =>
      DatabaseConfiguration(
        s"jdbc:h2:mem:chat-system-${uuid.toString.take(8)};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "",
        ""
      )
    }

  override val redisConfiguration: Resource[F, RedisConfiguration] =
    Resource.eval(freePort[F]).flatMap { port =>
      Resource
        .make {
          Sync[F]
            .blocking(RedisServer.builder().port(port).build())
            .flatTap(redisServer => Sync[F].blocking(redisServer.start()))
        } { redisServer =>
          Sync[F].blocking(redisServer.stop())
        }
        .as(RedisConfiguration("localhost", port, None))
    }

  override val kafkaConfiguration: Resource[F, KafkaConfiguration] =
    for {
      kafkaPort <- Resource.eval(freePort(EmbeddedKafkaConfig.defaultKafkaPort))
      zookeeperPort <- Resource.eval(freePort(EmbeddedKafkaConfig.defaultZookeeperPort))
      schemaRegistryPort <- Resource.eval(freePort(EmbeddedKafkaSchemaRegistryConfig.defaultSchemaRegistryPort))

      kafkaConfiguration = KafkaConfiguration(
        s"localhost:$kafkaPort",
        Uri(Some(Scheme.http), Some(Uri.Authority(port = Some(schemaRegistryPort))))
      )

      embeddedKafkaWithSR <- Resource.make(
        Sync[F]
          .blocking(
            EmbeddedKafka.start()(EmbeddedKafkaSchemaRegistryConfig(kafkaPort, zookeeperPort, schemaRegistryPort))
          )
      ) { kafka =>
        Sync[F].blocking(kafka.stop(false))
      }
    } yield kafkaConfiguration

}

object EmbeddedExternalServiceProvider {
  def freePort[F[_]: Sync]: F[Int] = freePort(20000)

  def freePort[F[_]: Sync](init: Int): F[Int] =
    RandomGenerator[F, Int](Random.nextInt() % 1000).generate
      .map(init + _)
      .flatMap { port =>
        MonadError[F, Throwable].handleErrorWith {
          Sync[F]
            .blocking(new ServerSocket(port))
            .flatMap(serverSocket => Sync[F].blocking(serverSocket.close()))
            .as(port)
        } { _ =>
          freePort(port)
        }
      }
}
