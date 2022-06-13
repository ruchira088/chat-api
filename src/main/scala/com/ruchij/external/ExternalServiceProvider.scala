package com.ruchij.external

import cats.effect.kernel.Resource
import com.ruchij.config.{KafkaConfiguration, MongoConfiguration, RedisConfiguration}
import com.ruchij.migration.config.DatabaseConfiguration

trait ExternalServiceProvider[F[_]] {
  val databaseConfiguration: Resource[F, DatabaseConfiguration]

  val redisConfiguration: Resource[F, RedisConfiguration]

  val kafkaConfiguration: Resource[F, KafkaConfiguration]

  val mongoConfiguration: Resource[F, MongoConfiguration]
}
