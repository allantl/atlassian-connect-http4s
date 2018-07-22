package com.allantl.atlassian.connect.http4s.auth.domain

import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest
import org.http4s.Uri

case class CanonicalUriHttpRequest(httpMethod: String, uri: Uri) extends CanonicalHttpRequest {

  override def method: String = httpMethod

  override def relativePath: String = {
    val relPath = uri.path
    if (relPath.isEmpty) "/" else relPath
  }

  override def parameterMap: Map[String, Seq[String]] = uri.multiParams
}
