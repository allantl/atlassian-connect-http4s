package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.errors

sealed trait JwtGeneratorError {
  def msg: String
}

case object RelativeUriError extends JwtGeneratorError {
  override def msg = "Url must be absolute"
}

case object InvalidSigningError extends JwtGeneratorError {
  override def msg = "Error when signing jwt"
}

case object BaseUrlMismatchError extends JwtGeneratorError {
  override def msg = "Invalid host base url"
}
