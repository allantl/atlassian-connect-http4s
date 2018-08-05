package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt

import cats.Monad
import com.allantl.atlassian.connect.http4s.configs.{AddOnProperties, AtlassianConnectConfig}
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra
import io.chrisdavenport.log4cats.Logger

class JwtAuthentication[F[_]: Monad: Logger: AtlassianHostRepositoryAlgebra](
    implicit acConfig: AtlassianConnectConfig,
    addOnProps: AddOnProperties) {

  val jwtAuthenticator = new JwtValidator[F]()
  val jwtGenerator = new JwtGenerator()
}
