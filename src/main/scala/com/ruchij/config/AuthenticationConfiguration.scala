package com.ruchij.config

import com.ruchij.config.AuthenticationConfiguration.ServiceAuthenticationConfiguration

case class AuthenticationConfiguration(serviceAuthentication: ServiceAuthenticationConfiguration)

object AuthenticationConfiguration {
  case class ServiceAuthenticationConfiguration(token: String)
}
