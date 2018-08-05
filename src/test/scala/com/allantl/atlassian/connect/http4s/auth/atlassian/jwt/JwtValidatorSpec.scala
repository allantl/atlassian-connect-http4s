package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt

import cats.Id
import com.allantl.atlassian.connect.http4s.AcHttp4sTest
import com.allantl.atlassian.connect.http4s.auth.domain.{CanonicalHttp4sHttpRequest, JwtCredentials}
import com.allantl.atlassian.connect.http4s.auth.errors.{InvalidJwt, JwtAuthenticationError, UnknownIssuer}
import com.allantl.atlassian.connect.http4s.configs.{AddOnProperties, AtlassianConnectConfig}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import com.allantl.atlassian.connect.http4s.mock.logging.NoLogging
import com.allantl.atlassian.connect.http4s.mock.repository.{NotFoundHostRepository, TestAtlassianHostRepository}
import org.http4s.{Method, Request, Uri}
import org.scalacheck.Prop

class JwtValidatorSpec extends AcHttp4sTest {

  implicit val acConfig = AtlassianConnectConfig(jwtExpirationTimeInSeconds = 5L, licenseCheck = false)
  implicit val addOnProps = AddOnProperties("com.allantl.http4s", "AcHttp4s", "https://com.allantl.http4s")
  implicit val logging = NoLogging[Id]()

  def withJwtCredentials[T](f: JwtCredentials => T)(
      implicit host: AtlassianHost,
      acConfig: AtlassianConnectConfig,
      uriImplicit: Option[Uri] = None): T = {
    val jwtGen = new JwtGenerator()
    val uriString = host.baseUrl + "/rest/api/2/issue?param1=true&param0=false"
    val uri = if (uriImplicit.isDefined) uriImplicit.get else Uri.unsafeFromString(uriString)
    lazy val tokenEither = jwtGen.generateToken("GET", uri, host)

    tokenEither must beRight

    val token = tokenEither.right.get
    val req = Request[Id](Method.GET, Uri.unsafeFromString(uriString))
    val jwtCredentials = JwtCredentials(token, CanonicalHttp4sHttpRequest(req))
    f(jwtCredentials)
  }

  "A JWT Authenticator" should {

    "fail if jwt cannot be parsed" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) { implicit host =>
      withJwtCredentials { jwtCredentials =>
        implicit val repo = new TestAtlassianHostRepository[Id]()
        val jwtAuthenticator = new JwtValidator[Id]()
        val invalid = jwtCredentials.copy(rawJwt = "invalidJwt")
        jwtAuthenticator.authenticate(invalid).value must beLeft
      }
    }

    "fail if jwt host could not be found" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) { implicit host =>
      withJwtCredentials { jwtCredentials =>
        implicit val repo = new NotFoundHostRepository[Id]()
        val jwtAuthenticator = new JwtValidator[Id]()
        jwtAuthenticator.authenticate(jwtCredentials).value must beLeft(
          UnknownIssuer(addOnProps.key): JwtAuthenticationError)
      }
    }

    "fail if qsh does not match" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) { implicit host =>
      implicit val repo = new TestAtlassianHostRepository[Id]()
      implicit val uri = Option(Uri.unsafeFromString(s"${host.baseUrl}/test/url"))
      val jwtAuthenticator = new JwtValidator[Id]()
      withJwtCredentials { jwtCredentials =>
        jwtAuthenticator.authenticate(jwtCredentials).value must beLeft(haveClass[InvalidJwt])
      }
    }

    "fail if jwt is expired" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) { implicit host =>
      val config = AtlassianConnectConfig(jwtExpirationTimeInSeconds = -1000L, licenseCheck = false)
      implicit val repo = new TestAtlassianHostRepository[Id]()
      val jwtAuthenticator = new JwtValidator[Id]()
      withJwtCredentials { jwtCredentials =>
        jwtAuthenticator.authenticate(jwtCredentials).value must beLeft(haveClass[InvalidJwt])
      }(host, config)
    }

    "successfully authenticate valid jwt" in Prop.forAll(atlassianHostGen(addOnProps.baseUrl)) { implicit host =>
      implicit val repo = new TestAtlassianHostRepository[Id]()
      val jwtAuthenticator = new JwtValidator[Id]()
      withJwtCredentials { jwtCredentials =>
        jwtAuthenticator.authenticate(jwtCredentials).value must beRight
      }
    }
  }

}
