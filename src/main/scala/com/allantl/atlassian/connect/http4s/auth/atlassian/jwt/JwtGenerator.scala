package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt

import java.net.{URI => JavaURI}
import java.time.Duration
import java.time.temporal.ChronoUnit

import cats.syntax.either._
import com.allantl.atlassian.connect.http4s.auth.domain.CanonicalUriHttpRequest
import com.allantl.atlassian.connect.http4s.auth.errors.{
  BaseUrlMismatchError,
  InvalidSigningError,
  JwtGeneratorError,
  RelativeUriError
}
import com.allantl.atlassian.connect.http4s.configs.{AddOnProperties, AtlassianConnectConfig}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import io.toolsplus.atlassian.jwt.api.Predef.RawJwt
import io.toolsplus.atlassian.jwt.{HttpRequestCanonicalizer, JwtBuilder}
import org.http4s.Uri

protected[http4s] class JwtGenerator(implicit acConfig: AtlassianConnectConfig, addOnProps: AddOnProperties) {

  def generateToken(httpMethod: String, uri: Uri, host: AtlassianHost): Either[JwtGeneratorError, String] =
    isAbsoluteUri(uri)
      .flatMap(isRequestToHost(_, host))
      .flatMap(createToken(httpMethod, _, host))

  private def createToken(httpMethod: String, uri: Uri, host: AtlassianHost): Either[JwtGeneratorError, RawJwt] = {
    val canonicalHttpRequest = CanonicalUriHttpRequest(httpMethod, uri)
    val queryHash = HttpRequestCanonicalizer.computeCanonicalRequestHash(canonicalHttpRequest)
    val expireAfter = Duration.of(acConfig.jwtExpirationTimeInSeconds, ChronoUnit.SECONDS)

    new JwtBuilder(expireAfter)
      .withIssuer(addOnProps.key)
      .withQueryHash(queryHash)
      .build(host.sharedSecret)
      .leftMap(_ => InvalidSigningError)
  }

  private def isAbsoluteUri(uri: Uri): Either[JwtGeneratorError, Uri] =
    if (toJavaUri(uri).isAbsolute) Right(uri) else Left(RelativeUriError)

  private def isRequestToHost(uri: Uri, host: AtlassianHost): Either[JwtGeneratorError, Uri] =
    for {
      hostUri <- Uri.fromString(host.baseUrl).leftMap(_ => BaseUrlMismatchError)
      reqToHost = !toJavaUri(hostUri).relativize(toJavaUri(uri)).isAbsolute
      uri <- if (reqToHost) uri.asRight else BaseUrlMismatchError.asLeft
    } yield uri

  private def toJavaUri(uri: Uri): JavaURI =
    new JavaURI(uri.renderString)

}

object JwtGenerator {
  implicit def fromJwtAuthenticator[F[_]](implicit jwt: JwtAuthentication[F]): JwtValidator[F] =
    jwt.jwtAuthenticator
}
