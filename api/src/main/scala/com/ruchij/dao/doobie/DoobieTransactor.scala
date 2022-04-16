package com.ruchij.dao.doobie

import cats.effect.Async
import cats.effect.kernel.Resource
import com.ruchij.migration.config.DatabaseConfiguration
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

import scala.concurrent.ExecutionContext

object DoobieTransactor {

  def create[F[_]: Async](databaseConfiguration: DatabaseConfiguration): Resource[F, HikariTransactor[F]] =
    for {
      executionContext <- ExecutionContexts.fixedThreadPool(8)
      hikariTransactor <- create[F](databaseConfiguration, executionContext)
    } yield hikariTransactor

  def create[F[_]: Async](
    databaseConfiguration: DatabaseConfiguration,
    connectEC: ExecutionContext
  ): Resource[F, HikariTransactor[F]] =
    for {
      databaseDriver <- Resource.eval(DatabaseDriver.parseFromConnectionUrl(databaseConfiguration.url))

      hikariTransactor <- HikariTransactor.newHikariTransactor(
        databaseDriver.driver,
        databaseConfiguration.url,
        databaseConfiguration.user,
        databaseConfiguration.password,
        connectEC
      )
    } yield hikariTransactor

}
