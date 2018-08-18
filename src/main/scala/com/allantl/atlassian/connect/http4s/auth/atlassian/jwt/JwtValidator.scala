package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt

import cats.Monad
import cats.data.EitherT
import cats.syntax.either._
import com.allantl.atlassian.connect.http4s.auth.domain.JwtCredentials
import com.allantl.atlassian.connect.http4s.auth.errors.{JwtAuthenticationError, JwtBadCredentials}
import com.allantl.atlassian.connect.http4s.auth.commons.JwtAuthenticatorUtils._
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra
import io.chrisdavenport.log4cats.Logger
import io.toolsplus.atlassian.jwt.Jwt

private[http4s] class JwtValidator[F[_]: Monad: Logger](
    implicit hostRepo: AtlassianHostRepositoryAlgebra[F]) {

  def authenticate(
      jwtCredentials: JwtCredentials
  ): EitherT[F, JwtAuthenticationError, AtlassianHostUser] =
    for {
      jwt <- parseJwt(jwtCredentials.rawJwt).toEitherT[F]
      clientKey <- extractClientKey(jwt).toEitherT[F]
      host <- EitherT(findInstalledHost(clientKey))
      verifiedToken <- EitherT(verifyJwt(jwtCredentials, host))
    } yield AtlassianHostUser(host, Option(verifiedToken.claims.getSubject))

  private def extractClientKey(jwt: Jwt): Either[JwtAuthenticationError, String] = {
    val unverifiedClaims = jwt.claims
    val hostClientKey: Either[JwtAuthenticationError, String] =
      Option(unverifiedClaims.getIssuer).toRight(
        JwtBadCredentials("Missing client key from claims")
      )
    hostClientKey
  }
}

object JwtValidator {
  implicit def fromJwtAuthenticator[F[_]](implicit jwt: JwtAuthentication[F]): JwtValidator[F] =
    jwt.jwtAuthenticator
}
