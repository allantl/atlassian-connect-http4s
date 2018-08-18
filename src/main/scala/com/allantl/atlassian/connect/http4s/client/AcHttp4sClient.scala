package com.allantl.atlassian.connect.http4s.client

import cats.Monad
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.JwtGenerator
import com.allantl.atlassian.connect.http4s.auth.errors.JwtGeneratorError
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import org.http4s.headers.Authorization
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Credentials, Request}

object AcHttp4sClient {

  val JwtAuthScheme = CaseInsensitiveString("JWT")

  implicit class AcAuthenticatedClient[F[_]: Monad](req: F[Request[F]]) {

    def acAuthenticated(
        implicit jwtGenerator: JwtGenerator,
        atlassianHost: AtlassianHost): F[Either[JwtGeneratorError, Request[F]]] =
      req.map { r =>
        val jwt = jwtGenerator.generateToken(r.method.name.capitalize, r.uri, atlassianHost)
        jwt.map(token => {
          val header = Authorization(Credentials.Token(JwtAuthScheme, token))
          r.withHeaders(r.headers.put(header))
        })
      }

    def acAuthenticatedQuick(
        implicit jwtGenerator: JwtGenerator,
        atlassianHost: AtlassianHost): F[Request[F]] =
      acAuthenticated.flatMap {
        _.fold(
          _ => req,
          r => r.pure[F]
        )
      }
  }
}
