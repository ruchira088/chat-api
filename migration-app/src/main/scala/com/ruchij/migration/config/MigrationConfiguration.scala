package com.ruchij.migration.config

import cats.{Applicative, ApplicativeThrow}
import pureconfig.ConfigObjectSource
import pureconfig.error.ConfigReaderException
import pureconfig.generic.auto._

case class MigrationConfiguration(databaseConfiguration: DatabaseConfiguration)

object MigrationConfiguration {
  def load[F[_]: ApplicativeThrow](configObjectSource: ConfigObjectSource): F[MigrationConfiguration] =
    configObjectSource.load[MigrationConfiguration]
      .fold[F[MigrationConfiguration]](
        configReaderFailures => ApplicativeThrow[F].raiseError(ConfigReaderException(configReaderFailures)),
        migrationConfiguration => Applicative[F].pure(migrationConfiguration)
      )
}

