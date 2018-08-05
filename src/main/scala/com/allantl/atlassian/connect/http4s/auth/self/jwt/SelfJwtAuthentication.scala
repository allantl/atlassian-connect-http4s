package com.allantl.atlassian.connect.http4s.auth.self.jwt

import cats.Monad
import com.allantl.atlassian.connect.http4s.configs.{AddOnProperties, AtlassianConnectConfig}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra
import io.chrisdavenport.log4cats.Logger
import cats.syntax.applicative._
import com.allantl.atlassian.connect.http4s.auth.errors.JwtGeneratorError

class SelfJwtAuthentication[F[_]: Monad: Logger: AtlassianHostRepositoryAlgebra]()(
    implicit addOnProps: AddOnProperties,
    acConfig: AtlassianConnectConfig) {

  val selfJwtAuth = new SelfJwtValidator[F]()
  val selfJwtGen = new SelfJwtGenerator()

  def generateToken()(implicit hostUser: AtlassianHostUser): F[Either[JwtGeneratorError, String]] =
    selfJwtGen.generateToken().pure[F]
}

object SelfJwtAuthentication {
  def apply[F[_]](implicit instance: SelfJwtAuthentication[F]): SelfJwtAuthentication[F] =
    instance
}
