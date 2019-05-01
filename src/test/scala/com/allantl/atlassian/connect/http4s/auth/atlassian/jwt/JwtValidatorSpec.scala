package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt

import cats.Id
import com.allantl.atlassian.connect.http4s.AcHttp4sTest
import com.allantl.atlassian.connect.http4s.auth.domain.{CanonicalHttp4sHttpRequest, JwtCredentials}
import com.allantl.atlassian.connect.http4s.auth.errors.{InvalidJwt, JwtAuthenticationError, UnknownIssuer}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import com.allantl.atlassian.connect.http4s.mock.repository.{NotFoundHostRepository, TestAtlassianHostRepository}
import com.allantl.jira4s.auth.{AcJwtConfig, AuthContext}
import com.allantl.jira4s.auth.jwt.JwtGenerator
import org.http4s.{Method, Request, Uri}
import org.scalacheck.Prop
import org.specs2.matcher.ThrownMessages

class JwtValidatorSpec extends AcHttp4sTest with ThrownMessages {

  case class AddOnProperties(key: String, baseUrl: String)

  implicit val addOnProps = AddOnProperties("com.allantl.http4s", "https://com.allantl.http4s")
  implicit val acConfig = AcJwtConfig(addOnProps.key, 5L)

  case class RequestUri(value: String)

  private def withJwtCredentials[T](f: JwtCredentials => T)(
      implicit host: AtlassianHost,
      acConfig: AcJwtConfig,
      reqUri: Option[RequestUri] = None
  ): T = {
    val tokenUri =
      host.baseUrl + "/rest/api/2/issue?param1=true&param0=false"

    implicit val authContext = new AuthContext {
      override def instanceUrl: String = host.baseUrl
      override def accessToken: String = host.sharedSecret
    }

    JwtGenerator.generateToken(httpMethod = "GET", tokenUri) match {
      case Left(err) =>
        fail(s"Error generating jwt token: ${err.getMessage}")

      case Right(jwt) =>
        val requestUri = reqUri.map(_.value).getOrElse(tokenUri)
        val req = Request[Id](Method.GET, Uri.unsafeFromString(requestUri))
        val jwtCredentials = JwtCredentials(jwt, CanonicalHttp4sHttpRequest(req))
        f(jwtCredentials)
    }
  }

  "A JWT Authenticator" should {

    "fail if jwt cannot be parsed" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) {
      implicit host =>
        withJwtCredentials { jwtCredentials =>
          implicit val repo = new TestAtlassianHostRepository[Id]()
          val jwtAuthenticator = new JwtValidator[Id]()
          val invalid = jwtCredentials.copy(rawJwt = "invalidJwt")
          jwtAuthenticator.authenticate(invalid) must beLeft
        }
    }

    "fail if jwt host could not be found" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) {
      implicit host =>
        withJwtCredentials { jwtCredentials =>
          implicit val repo = new NotFoundHostRepository[Id]()
          val jwtAuthenticator = new JwtValidator[Id]()
          jwtAuthenticator.authenticate(jwtCredentials) must beLeft(
            UnknownIssuer(addOnProps.key): JwtAuthenticationError)
        }
    }

    "fail if qsh does not match" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) {
      implicit host =>
        implicit val repo = new TestAtlassianHostRepository[Id]()
        implicit val requestUri: Option[RequestUri] = Some(RequestUri(s"${host.baseUrl}/test/url"))
        val jwtAuthenticator = new JwtValidator[Id]()
        withJwtCredentials { jwtCredentials =>
          jwtAuthenticator.authenticate(jwtCredentials) must beLeft(haveClass[InvalidJwt])
        }
    }

    "fail if jwt is expired" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) { implicit host =>
      val config = acConfig.copy(jwtExpirationInSeconds = -1000L)
      implicit val repo = new TestAtlassianHostRepository[Id]()
      val jwtAuthenticator = new JwtValidator[Id]()
      withJwtCredentials { jwtCredentials =>
        jwtAuthenticator.authenticate(jwtCredentials) must beLeft(haveClass[InvalidJwt])
      }(host, config)
    }

    "successfully authenticate valid jwt" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) {
      implicit host =>
        implicit val repo = new TestAtlassianHostRepository[Id]()
        val jwtAuthenticator = new JwtValidator[Id]()
        withJwtCredentials { jwtCredentials =>
          jwtAuthenticator.authenticate(jwtCredentials) must beRight
        }
    }
  }
}
