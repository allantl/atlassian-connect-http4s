package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt

import com.allantl.atlassian.connect.http4s.AcHttp4sTest
import com.allantl.atlassian.connect.http4s.auth.errors.{BaseUrlMismatchError, JwtGeneratorError, RelativeUriError}
import com.allantl.atlassian.connect.http4s.configs.{AddOnProperties, AtlassianConnectConfig}
import io.toolsplus.atlassian.jwt.JwtParser
import org.http4s.Uri
import org.scalacheck.Prop

class JwtGeneratorSpec extends AcHttp4sTest {

  implicit val acConfig = AtlassianConnectConfig(jwtExpirationTimeInSeconds = 5L)
  implicit val addonProperties = AddOnProperties("com.allantl.http4s", "AcHttp4s", "https://com.allantl.http4s")
  val jwtGen = new JwtGenerator()

  "A JWT generator" should {
    "fail if URI does not match with host" in Prop.forAll(atlassianHostGen(addonProperties.baseUrl)) { host =>
      val uri = Uri.unsafeFromString("https://atlassian.com")
      jwtGen.generateToken("GET", uri, host) must beLeft(BaseUrlMismatchError: JwtGeneratorError)
    }

    "fail if URI is not absolute" in Prop.forAll(atlassianHostGen(addonProperties.baseUrl)) { host =>
      val uri = Uri.unsafeFromString("atlassian.com")
      jwtGen.generateToken("GET", uri, host) must beLeft(RelativeUriError: JwtGeneratorError)
    }

    "generate jwt successfully" in Prop.forAll(atlassianHostGen(addonProperties.baseUrl)) { host =>
      val uri = Uri.unsafeFromString(host.baseUrl)
      lazy val token = jwtGen.generateToken("GET", uri, host)
      lazy val jwtEither = JwtParser.parse(token.right.get)
      lazy val jwt = jwtEither.right.get
      lazy val qsh = Option(jwt.claims.getClaim("qsh")).map(_.asInstanceOf[String])

      token must beRight
      jwtEither must beRight
      qsh must not beEmpty
    }

    "set expiry token correctly" in Prop.forAll(atlassianHostGen(addonProperties.baseUrl)) { host =>
      val uri = Uri.unsafeFromString(host.baseUrl)
      lazy val token = jwtGen.generateToken("GET", uri, host)
      lazy val jwtEither = JwtParser.parse(token.right.get)
      lazy val jwt = jwtEither.right.get

      lazy val now = System.currentTimeMillis / 1000
      lazy val expiry = jwt.claims.getExpirationTime.getTime / 1000
      lazy val expectedExpiry = now + acConfig.jwtExpirationTimeInSeconds

      token must beRight
      jwtEither must beRight
      expiry must beCloseTo(expectedExpiry +/- 2)
    }
  }
}
