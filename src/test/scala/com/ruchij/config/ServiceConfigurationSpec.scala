package com.ruchij.config

import cats.effect.IO
import com.comcast.ip4s.IpLiteralSyntax
import com.ruchij.config.AuthenticationConfiguration.ServiceAuthenticationConfiguration
import com.ruchij.migration.config.DatabaseConfiguration
import com.ruchij.test.utils.IOUtils.{IOErrorOps, runIO}
import org.http4s.implicits.http4sLiteralsSyntax
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import pureconfig.ConfigSource

class ServiceConfigurationSpec extends AnyFlatSpec with Matchers {

  "ServiceConfiguration" should "parse the ConfigObjectSource" in runIO {
    val configObjectSource =
      ConfigSource.string {
        s"""
          database-configuration {
            url = "jdbc:h2:mem:chat-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"
            url = $${?DATABASE_URL}

            user = "my-user"
            user = $${?DATABASE_USER}

            password = "my-password"
            password = $${?DATABASE_PASSWORD}
          }

          mongo-configuration {
            connection-string = "mongodb://mongo:27017"
            connection-string = $${?MONGO_URL}

            database = "chat-api"
            database = $${?MONGO_DATABASE}
          }

          file-store-configuration {
            root = "/opt/images"
            root = $${?FILE_STORE_ROOT}
          }

          redis-configuration {
            hostname = "localhost"
            hostname = $${?REDIS_HOSTNAME}

            port = 6379
            port = $${?REDIS_PORT}

            password = $${?REDIS_PASSWORD}
          }

          kafka-configuration {
            bootstrap-servers = "kafka-broker:9092"

            schema-registry = "https://schema-registry:8081"
          }

          http-configuration {
            host = "127.0.0.1"
            host = $${?HTTP_HOST}

            port = 80
            port = $${?HTTP_PORT}
          }

          instance-configuration {
             hostname = "localhost"
             hostname = $${?CHAT_API_SERVICE_SERVICE_HOST}

             port = 8000
             port = $${?HTTP_PORT}
          }

          authentication-configuration {
            service-authentication {
                token = "my-token"
                token = $${?SERVICE_TOKEN}
            }
          }

          build-information {
            git-branch = "my-branch"

            git-commit = $${?GIT_COMMIT}

            build-timestamp = "2021-07-31T10:10:00.000Z"
          }
        """
      }

    ServiceConfiguration.parse[IO](configObjectSource).flatMap { serviceConfiguration =>
      IO.delay {
        serviceConfiguration.databaseConfiguration mustBe DatabaseConfiguration(
          "jdbc:h2:mem:chat-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
          "my-user",
          "my-password"
        )

        serviceConfiguration.redisConfiguration mustBe RedisConfiguration("localhost", 6379, None)

        serviceConfiguration.httpConfiguration mustBe HttpConfiguration(ipv4"127.0.0.1", port"80")

        serviceConfiguration.kafkaConfiguration mustBe KafkaConfiguration("kafka-broker:9092", uri"https://schema-registry:8081")

        serviceConfiguration.instanceConfiguration mustBe InstanceConfiguration("localhost", 8000)

        serviceConfiguration.authenticationConfiguration mustBe
          AuthenticationConfiguration(ServiceAuthenticationConfiguration("my-token"))

        serviceConfiguration.mongoConfiguration mustBe MongoConfiguration("mongodb://mongo:27017", "chat-api")

        serviceConfiguration.fileStoreConfiguration mustBe FileStoreConfiguration("/opt/images")

        serviceConfiguration.buildInformation mustBe
          BuildInformation(Some("my-branch"), None, Some(new DateTime(2021, 7, 31, 10, 10, 0, 0, DateTimeZone.UTC)))
      }
    }
  }

  it should "return an error if ConfigObjectSource is not parsable" in runIO {
    val configObjectSource =
      ConfigSource.string {
        s"""
          http-configuration {
            host = "0.0.0.0"

            port = 8000
          }

          build-information {
            git-branch = "my-branch"

            build-timestamp = "invalid-date"
          }
        """
      }

    ServiceConfiguration
      .parse[IO](configObjectSource)
      .error
      .flatMap { throwable =>
        IO.delay {
          throwable.getMessage must include(
            "Cannot convert 'invalid-date' to DateTime: Invalid format: \"invalid-date\""
          )
        }
      }
  }

}
