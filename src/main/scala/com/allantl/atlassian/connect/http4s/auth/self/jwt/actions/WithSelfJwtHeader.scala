package com.allantl.atlassian.connect.http4s.auth.self.jwt.actions
import cats.Functor
import com.allantl.atlassian.connect.http4s.auth.self.jwt.SelfJwtGenerator
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import org.http4s.{Credentials, Response}
import cats.syntax.functor._
import com.allantl.atlassian.connect.http4s.client.AcHttp4sClient.JwtAuthScheme
import org.http4s.headers.Authorization

object WithSelfJwtHeader {

  def apply[F[_]: Functor](resp: F[Response[F]])(
      implicit selfJwtGenerator: SelfJwtGenerator,
      ahu: AtlassianHostUser
  ): F[Response[F]] =
    resp.map { r =>
      selfJwtGenerator.generateToken() match {
        case Left(_) => r
        case Right(token) =>
          r.putHeaders(Authorization(Credentials.Token(JwtAuthScheme, token)))
      }
    }
}
