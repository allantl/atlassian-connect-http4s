package com.allantl.atlassian.connect.http4s.auth.self.jwt

import cats.Id
import com.allantl.atlassian.connect.http4s.AcHttp4sTest
import com.allantl.atlassian.connect.http4s.auth.domain.{CanonicalHttp4sHttpRequest, JwtCredentials}
import com.allantl.atlassian.connect.http4s.configs.{AddOnProperties, AtlassianConnectConfig}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import com.allantl.atlassian.connect.http4s.mock.logging.NoLogging
import com.allantl.atlassian.connect.http4s.mock.repository.TestAtlassianHostRepository
import org.http4s.{Method, Request, Uri}
import org.scalacheck.Prop

class SelfJwtSpec extends AcHttp4sTest {

  implicit val addonProperties = AddOnProperties("com.allantl.http4s", "AcHttp4s", "https://com.allantl.http4s")
  implicit val logging = NoLogging[Id]()

  "Self Jwt Generator & Authenticator" should {
    "be able to encode and decode jwt successfully" in Prop.forAll(atlassianHostGen(addonProperties.baseUrl)) {
      implicit host =>
        implicit val acConfig = AtlassianConnectConfig(jwtExpirationTimeInSeconds = 5L, licenseCheck = false)
        implicit val repo = new TestAtlassianHostRepository()
        implicit val ahu = AtlassianHostUser(host, None)

        val jwtGen = new SelfJwtGenerator()
        val jwtAuth = new SelfJwtValidator[Id]()
        lazy val jwtEither = jwtGen.generateToken()

        jwtEither must beRight

        lazy val jwt = jwtEither.right.get
        val req = Request[Id](Method.GET, Uri.unsafeFromString(host.baseUrl + "/rest/api/2/issue"))
        val decoded = jwtAuth.authenticate(JwtCredentials(jwt, CanonicalHttp4sHttpRequest(req)))

        decoded.value must beRight
    }

    "fail authentication if jwt is expired" in Prop.forAll(atlassianHostGen(addonProperties.baseUrl)) { implicit host =>
      implicit val acConfig = AtlassianConnectConfig(jwtExpirationTimeInSeconds = -100L, licenseCheck = false)
      implicit val repo = new TestAtlassianHostRepository()
      implicit val ahu = AtlassianHostUser(host, None)

      val jwtGen = new SelfJwtGenerator()
      val jwtAuth = new SelfJwtValidator[Id]()
      lazy val jwtEither = jwtGen.generateToken()

      jwtEither must beRight

      lazy val jwt = jwtEither.right.get
      val req = Request[Id](Method.GET, Uri.unsafeFromString(host.baseUrl + "/rest/api/2/issue"))
      val decoded = jwtAuth.authenticate(JwtCredentials(jwt, CanonicalHttp4sHttpRequest(req)))

      decoded.value must beLeft
    }
  }

}
