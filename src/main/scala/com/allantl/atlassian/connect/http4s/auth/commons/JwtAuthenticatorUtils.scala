package com.allantl.atlassian.connect.http4s.auth.commons

import cats.{Applicative, Monad}
import com.allantl.atlassian.connect.http4s.auth.domain.JwtCredentials
import com.allantl.atlassian.connect.http4s.auth.errors.{
  InvalidJwt,
  JwtAuthenticationError,
  UnknownIssuer
}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import io.toolsplus.atlassian.jwt.{HttpRequestCanonicalizer, Jwt, JwtParser, JwtReader}
import cats.implicits._
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra

object JwtAuthenticatorUtils {

  def parseJwt[F[_]: Monad](rawJwt: String): F[Either[JwtAuthenticationError, Jwt]] =
    for {
      jwtOrErr <- JwtParser.parse(rawJwt).pure[F]
      res <- jwtOrErr.fold(
        e => InvalidJwt(e.getMessage()).asLeft[Jwt].pure[F],
        jwt => jwt.asRight[JwtAuthenticationError].pure[F]
      )
    } yield res

  def findInstalledHost[F[_]: Monad](clientKey: String)(
      implicit hostRepo: AtlassianHostRepositoryAlgebra[F])
    : F[Either[JwtAuthenticationError, AtlassianHost]] =
    hostRepo
      .findByClientKey(clientKey)
      .flatMap {
        case Some(host) => host.asRight[JwtAuthenticationError].pure[F]
        case None => UnknownIssuer(clientKey).asLeft[AtlassianHost].leftWiden[JwtAuthenticationError].pure[F]
      }

  def verifyJwt[F[_]: Applicative](
      jwtCredentials: JwtCredentials,
      host: AtlassianHost): F[Either[JwtAuthenticationError, Jwt]] = {
    val qsh =
      HttpRequestCanonicalizer.computeCanonicalRequestHash(jwtCredentials.canonicalHttpRequest)
    JwtReader(host.sharedSecret).readAndVerify(jwtCredentials.rawJwt, qsh) match {
      case Left(e) => InvalidJwt(e.getMessage).asLeft[Jwt].leftWiden[JwtAuthenticationError].pure[F]
      case Right(jwt) => jwt.asRight[JwtAuthenticationError].pure[F]
    }
  }
}
