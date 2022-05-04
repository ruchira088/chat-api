package com.ruchij.config

import cats.ApplicativeError
import com.ruchij.config.BuildInformation
import com.ruchij.config.ConfigReaders.{dateTimeConfigReader, uriConfigReader}
import com.ruchij.migration.config.DatabaseConfiguration
import com.ruchij.types.FunctionKTypes._
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class ServiceConfiguration(
  databaseConfiguration: DatabaseConfiguration,
  redisConfiguration: RedisConfiguration,
  kafkaConfiguration: KafkaConfiguration,
  httpConfiguration: HttpConfiguration,
  instanceConfiguration: InstanceConfiguration,
  authenticationConfiguration: AuthenticationConfiguration,
  buildInformation: BuildInformation
)

object ServiceConfiguration {
  def parse[F[_]: ApplicativeError[*[_], Throwable]](configObjectSource: ConfigObjectSource): F[ServiceConfiguration] =
    configObjectSource.load[ServiceConfiguration].left.map(ConfigReaderException.apply).toType[F, Throwable]
}
