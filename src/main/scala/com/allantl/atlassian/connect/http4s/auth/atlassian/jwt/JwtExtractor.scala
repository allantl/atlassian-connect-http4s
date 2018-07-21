package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt

import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.domain.{CanonicalHttp4sHttpRequest, JwtCredentials}
import org.http4s.Request
import org.http4s.headers.Authorization

object JwtExtractor {

  def extractJwt[F[_]](req: Request[F]): Option[JwtCredentials] =
    extractFromHeader(req)
      .orElse(extractFromQueryParam(req))
      .map(JwtCredentials(_, CanonicalHttp4sHttpRequest(req)))

  private def extractFromHeader[F[_]](req: Request[F]): Option[String] =
    req.headers
      .find(_.is(Authorization))
      .map(_.value)
      .filter(h => h.nonEmpty && h.startsWith("JWT"))
      .map(_.substring("JWT".length).trim)

  private def extractFromQueryParam[F[_]](req: Request[F]): Option[String] =
    req.params
      .get("jwt")
      .filter(_.nonEmpty)
}
