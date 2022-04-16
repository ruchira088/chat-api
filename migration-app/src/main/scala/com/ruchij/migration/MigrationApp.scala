package com.ruchij.migration

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.Sync
import com.ruchij.migration.config.{DatabaseConfiguration, MigrationConfiguration}
import com.typesafe.scalalogging.Logger
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import pureconfig.ConfigSource

object MigrationApp extends IOApp {

  private val logger = Logger[MigrationApp.type]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- IO.blocking(logger.info("Starting migration application..."))

      configObjectSource <- IO.delay(ConfigSource.defaultApplication)
      migrationConfiguration <- MigrationConfiguration.load[IO](configObjectSource)
      migrationResult <- migrate[IO](migrationConfiguration.databaseConfiguration)

      _ <- IO.blocking(logger.info(s"Migration completed: schema version ${migrationResult.targetSchemaVersion}"))
    }
    yield ExitCode.Success

  def migrate[F[_]: Sync](databaseConfiguration: DatabaseConfiguration): F[MigrateResult] =
    Sync[F].blocking {
      Flyway
        .configure()
        .dataSource(databaseConfiguration.url, databaseConfiguration.user, databaseConfiguration.password)
        .load()
        .migrate()
    }

}
