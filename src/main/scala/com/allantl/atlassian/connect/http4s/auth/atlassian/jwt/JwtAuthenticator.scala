package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt

import cats.data.EitherT
import cats.effect.Sync
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.domain.JwtCredentials
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra
import io.toolsplus.atlassian.jwt.{HttpRequestCanonicalizer, Jwt, JwtParser, JwtReader}
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.apply._
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.errors.{
  InvalidJwt,
  JwtAuthenticationError,
  JwtBadCredentials,
  UnknownIssuer
}
import com.allantl.atlassian.connect.http4s.domain.{AtlassianHost, AtlassianHostUser}
import io.chrisdavenport.log4cats.Logger

class JwtAuthenticator[F[_]: Sync: Logger](hostRepo: AtlassianHostRepositoryAlgebra[F]) {

  def authenticate(jwtCredentials: JwtCredentials): EitherT[F, JwtAuthenticationError, AtlassianHostUser] =
    for {
      jwt <- parseJwt(jwtCredentials.rawJwt).toEitherT[F]
      clientKey <- extractClientKey(jwt).toEitherT[F]
      host <- EitherT(findInstalledHost(clientKey))
      verifiedToken <- EitherT(verifyJwt(jwtCredentials, host))
    } yield AtlassianHostUser(host, Option(verifiedToken.claims.getSubject))

  private def parseJwt(rawJwt: String): Either[JwtAuthenticationError, Jwt] =
    JwtParser.parse(rawJwt).leftMap { e =>
      Logger[F].error(s"Parsing of JWT failed: $e")
      InvalidJwt(e.getMessage())
    }

  private def extractClientKey(jwt: Jwt): Either[JwtAuthenticationError, String] = {
    val unverifiedClaims = jwt.claims
    val hostClientKey: Either[JwtAuthenticationError, String] =
      Option(unverifiedClaims.getIssuer).toRight(
        JwtBadCredentials("Missing client key from claims")
      )
    hostClientKey
  }

  private def findInstalledHost(clientKey: String): F[Either[JwtAuthenticationError, AtlassianHost]] =
    hostRepo
      .findByClientKey(clientKey)
      .flatMap {
        case Some(host) => host.asRight[JwtAuthenticationError].pure[F]
        case None =>
          val err: Either[JwtAuthenticationError, AtlassianHost] =
            UnknownIssuer(clientKey).asLeft[AtlassianHost]
          Logger[F].error(s"Could not find an installed host for the provided client key: $clientKey") *> err.pure[F]
      }

  private def verifyJwt(jwtCredentials: JwtCredentials, host: AtlassianHost): F[Either[JwtAuthenticationError, Jwt]] = {
    val qsh = HttpRequestCanonicalizer.computeCanonicalRequestHash(jwtCredentials.canonicalHttpRequest)
    JwtReader(host.sharedSecret).readAndVerify(jwtCredentials.rawJwt, qsh) match {
      case Left(e) =>
        val err: Either[JwtAuthenticationError, Jwt] = InvalidJwt(e.getMessage).asLeft
        Logger[F].error(s"Reading and validating of JWT failed: $e") *> err.pure[F]
      case Right(jwt) => jwt.asRight[JwtAuthenticationError].pure[F]
    }
  }
}
