package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt

import cats.Monad
import cats.data.EitherT
import cats.syntax.either._
import com.allantl.atlassian.connect.http4s.auth.commons.JwtAuthenticatorUtils._
import com.allantl.atlassian.connect.http4s.auth.domain.JwtCredentials
import com.allantl.atlassian.connect.http4s.auth.errors.{JwtAuthenticationError, JwtBadCredentials}
import com.allantl.atlassian.connect.http4s.domain.{AtlassianConnectContext, AtlassianHostUser}
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra
import io.chrisdavenport.log4cats.Logger
import io.circe.parser._
import io.toolsplus.atlassian.jwt.Jwt

final class JwtValidator[F[_]: Monad: Logger: AtlassianHostRepositoryAlgebra]() {

  def authenticate(
      jwtCredentials: JwtCredentials
  ): F[Either[JwtAuthenticationError, AtlassianHostUser]] = {
    val ahuT = for {
      jwt <- EitherT(parseJwt(jwtCredentials.rawJwt))
      clientKey <- extractClientKey(jwt).toEitherT[F]
      host <- EitherT(findInstalledHost(clientKey))
      verifiedToken <- EitherT(verifyJwt(jwtCredentials, host))
      maybeRawUserCtx = Option(verifiedToken.claims.getClaims.get("context")).map(_.toString)
      acCtx = maybeRawUserCtx.flatMap(parse(_).right.flatMap(_.as[AtlassianConnectContext]).toOption)
    } yield AtlassianHostUser(host, acCtx.map(_.user))

    ahuT.value
  }

  private def extractClientKey(jwt: Jwt): Either[JwtAuthenticationError, String] = {
    val unverifiedClaims = jwt.claims
    val hostClientKey: Either[JwtAuthenticationError, String] =
      Option(unverifiedClaims.getIssuer).toRight(
        JwtBadCredentials("Missing client key from claims")
      )
    hostClientKey
  }
}
