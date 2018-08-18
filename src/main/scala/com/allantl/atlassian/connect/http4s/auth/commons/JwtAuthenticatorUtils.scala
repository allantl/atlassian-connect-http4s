package com.allantl.atlassian.connect.http4s.auth.commons

import cats.{Applicative, Monad}
import com.allantl.atlassian.connect.http4s.auth.domain.JwtCredentials
import com.allantl.atlassian.connect.http4s.auth.errors.{
  InvalidJwt,
  JwtAuthenticationError,
  UnknownIssuer
}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import io.chrisdavenport.log4cats.Logger
import io.toolsplus.atlassian.jwt.{HttpRequestCanonicalizer, Jwt, JwtParser, JwtReader}
import cats.syntax.either._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.apply._
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra

object JwtAuthenticatorUtils {

  def parseJwt[F[_]: Logger](rawJwt: String): Either[JwtAuthenticationError, Jwt] =
    JwtParser.parse(rawJwt).leftMap { e =>
      Logger[F].error(s"Parsing of JWT failed: $e")
      InvalidJwt(e.getMessage())
    }

  def findInstalledHost[F[_]: Monad: Logger](clientKey: String)(
      implicit hostRepo: AtlassianHostRepositoryAlgebra[F])
    : F[Either[JwtAuthenticationError, AtlassianHost]] =
    hostRepo
      .findByClientKey(clientKey)
      .flatMap {
        case Some(host) => host.asRight[JwtAuthenticationError].pure[F]
        case None =>
          val err: Either[JwtAuthenticationError, AtlassianHost] =
            UnknownIssuer(clientKey).asLeft[AtlassianHost]
          Logger[F].error(
            s"Could not find an installed host for the provided client key: $clientKey") *>
            err.pure[F]
      }

  def verifyJwt[F[_]: Applicative: Logger](
      jwtCredentials: JwtCredentials,
      host: AtlassianHost): F[Either[JwtAuthenticationError, Jwt]] = {
    val qsh =
      HttpRequestCanonicalizer.computeCanonicalRequestHash(jwtCredentials.canonicalHttpRequest)
    JwtReader(host.sharedSecret).readAndVerify(jwtCredentials.rawJwt, qsh) match {
      case Left(e) =>
        val err: Either[JwtAuthenticationError, Jwt] = InvalidJwt(e.getMessage).asLeft
        Logger[F].error(s"Reading and validating of JWT failed: $e") *> err.pure[F]
      case Right(jwt) => jwt.asRight[JwtAuthenticationError].pure[F]
    }
  }
}
