package com.allantl.atlassian.connect.http4s.auth.errors

sealed trait JwtAuthenticationError {
  def message: String
}

final case class JwtBadCredentials(message: String) extends JwtAuthenticationError

final case class InvalidJwt(message: String) extends JwtAuthenticationError

final case class UnknownIssuer(issuer: String) extends JwtAuthenticationError {
  override val message = s"Could not find an installed host for the provided client key: $issuer"
}

case object JwtNotFound extends JwtAuthenticationError {
  override val message = "No jwt token found"
}
