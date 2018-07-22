package com.allantl.atlassian.connect.http4s.auth.domain

import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest
import org.http4s.Request

/**
  * @see https://developer.atlassian.com/blog/2015/01/understanding-jwt/
  */
case class CanonicalHttp4sHttpRequest[F[_]](req: Request[F]) extends CanonicalHttpRequest {

  override def method: String = req.method.name

  override def relativePath: String = {
    val path = req.pathInfo
    if (path.isEmpty) "/" else path
  }

  override def parameterMap: Map[String, Seq[String]] = req.multiParams
}
