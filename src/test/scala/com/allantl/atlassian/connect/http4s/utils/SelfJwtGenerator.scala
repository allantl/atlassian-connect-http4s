package com.allantl.atlassian.connect.http4s.utils

import java.time.Duration
import java.time.temporal.ChronoUnit

import com.allantl.atlassian.connect.http4s.auth.errors.{InvalidSigningError, JwtGeneratorError}
import com.allantl.atlassian.connect.http4s.configs.{AddOnProperties, AtlassianConnectConfig}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import io.toolsplus.atlassian.jwt.JwtBuilder
import cats.syntax.bifunctor._
import cats.instances.either._

private[http4s] class SelfJwtGenerator(
    implicit addOnProperties: AddOnProperties,
    acConfig: AtlassianConnectConfig
) {

  private val ClientKeyClaim = "clientKey"

  def generateToken()(implicit hostUser: AtlassianHostUser): Either[JwtGeneratorError, String] = {
    val expirationAfter = Duration.of(acConfig.jwtExpirationTimeInSeconds, ChronoUnit.SECONDS)
    val jwt = new JwtBuilder(expirationAfter)
      .withIssuer(addOnProperties.key)
      .withAudience(Seq(addOnProperties.key))
      .withClaim(ClientKeyClaim, hostUser.host.clientKey)
    hostUser.userKey.map(jwt.withSubject)
    jwt.build(hostUser.host.sharedSecret).leftMap(_ => InvalidSigningError)
  }
}
