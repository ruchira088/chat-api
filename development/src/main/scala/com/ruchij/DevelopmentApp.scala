package com.ruchij

import cats.Monad
import cats.effect.kernel.Resource
import cats.effect.{ExitCode, IO, IOApp}
import com.ruchij.config.AuthenticationConfiguration.ServiceAuthenticationConfiguration
import com.ruchij.config._
import com.ruchij.external.ExternalServiceProvider
import com.ruchij.external.embedded.EmbeddedExternalServiceProvider
import com.ruchij.types.FunctionKTypes.ioFutureToIO
import com.ruchij.types.JodaClock

object DevelopmentApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    initialize[IO](new EmbeddedExternalServiceProvider[IO])
      .use { serviceConfiguration => ApiApp.run[IO](serviceConfiguration) }

  def initialize[F[_]: Monad: JodaClock](
    externalServiceProvider: ExternalServiceProvider[F]
  ): Resource[F, ServiceConfiguration] =
    for {
      databaseConfiguration <- externalServiceProvider.databaseConfiguration
      redisConfiguration <- externalServiceProvider.redisConfiguration
      kafkaConfiguration <- externalServiceProvider.kafkaConfiguration
      timestamp <- Resource.eval(JodaClock[F].timestamp)
      httpConfiguration = HttpConfiguration("0.0.0.0", 8000)
      instanceConfiguration = InstanceConfiguration("localhost", 8000)
      authenticationConfiguration = AuthenticationConfiguration(ServiceAuthenticationConfiguration("my-service-token"))
      buildInformation = BuildInformation(Some("my-branch"), Some("my-commit"), Some(timestamp))

      serviceConfiguration = ServiceConfiguration(
        databaseConfiguration,
        redisConfiguration,
        kafkaConfiguration,
        httpConfiguration,
        instanceConfiguration,
        authenticationConfiguration,
        buildInformation
      )
    } yield serviceConfiguration

}
