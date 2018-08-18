package com.allantl.atlassian.connect.http4s.auth.self.jwt

import cats.Monad
import cats.data.EitherT
import cats.syntax.either._
import com.allantl.atlassian.connect.http4s.auth.domain.JwtCredentials
import com.allantl.atlassian.connect.http4s.auth.errors.{JwtAuthenticationError, JwtBadCredentials}
import com.allantl.atlassian.connect.http4s.auth.commons.JwtAuthenticatorUtils._
import com.allantl.atlassian.connect.http4s.configs.AddOnProperties
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra
import com.nimbusds.jwt.JWTClaimsSet
import io.chrisdavenport.log4cats.Logger
import io.toolsplus.atlassian.jwt.Jwt

import scala.collection.JavaConverters._

private[http4s] class SelfJwtValidator[F[_]: Monad: Logger]()(
    implicit addOnProps: AddOnProperties,
    hostRepo: AtlassianHostRepositoryAlgebra[F]) {

  def authenticate(
      jwtCredentials: JwtCredentials
  ): EitherT[F, JwtAuthenticationError, AtlassianHostUser] =
    for {
      jwt <- parseJwt(jwtCredentials.rawJwt).toEitherT[F]
      clientKey <- extractClientKey(jwt).toEitherT[F]
      host <- EitherT(findInstalledHost(clientKey))
      verifiedToken <- EitherT(verifyJwt(jwtCredentials, host))
    } yield AtlassianHostUser(host, Option(verifiedToken.claims.getSubject))

  private def extractClientKey(jwt: Jwt): Either[JwtAuthenticationError, String] = {
    val unverifiedClaims = jwt.claims
    if (isSelfToken(addOnProps.key, unverifiedClaims)) {
      validateAudience(addOnProps.key, unverifiedClaims).flatMap { _ =>
        clientKeyFromJwtClaims(unverifiedClaims)
      }
    } else {
      Left(JwtBadCredentials(s"Invalid issuer for token: $jwt"))
    }
  }

  private def isSelfToken(addonKey: String, unverifiedClaims: JWTClaimsSet): Boolean =
    addonKey == unverifiedClaims.getIssuer

  private def validateAudience(
      addonKey: String,
      unverifiedClaims: JWTClaimsSet): Either[JwtAuthenticationError, List[String]] =
    unverifiedClaims.getAudience.asScala.toList match {
      case audience @ maybeAddonKey :: Nil =>
        if (maybeAddonKey == addonKey) Right(audience)
        else Left(JwtBadCredentials(s"Invalid audience $maybeAddonKey"))
      case audience =>
        Left(JwtBadCredentials(s"Invalid audience: $audience"))
    }

  private def clientKeyFromJwtClaims(
      unverifiedClaims: JWTClaimsSet): Either[JwtAuthenticationError, String] = {
    val maybeClientKeyClaim = Option(
      unverifiedClaims
        .getClaim(ClientKeyClaim)
        .asInstanceOf[String]
    )
    maybeClientKeyClaim.fold(JwtBadCredentials("Missing client key claim").asLeft[String])(Right(_))
  }
}

object SelfJwtValidator {
  implicit def fromSelfJwtAuthentication[F[_]](
      implicit selfJwtAuthentication: SelfJwtAuthentication[F]): SelfJwtValidator[F] =
    selfJwtAuthentication.selfJwtAuth
}
