package com.allantl.atlassian.connect.http4s.auth.self.jwt.actions

import cats.Monad
import cats.data.EitherT
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.JwtExtractor
import com.allantl.atlassian.connect.http4s.auth.errors.JwtNotFound
import com.allantl.atlassian.connect.http4s.auth.self.jwt.SelfJwtValidator
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import org.http4s.{Request, Response, Status}
import cats.syntax.flatMap._

object SelfJwtAuthenticated {

  def apply[F[_]: Monad](req: Request[F])(f: AtlassianHostUser => F[Response[F]])(
      implicit selfJwtAuthenticator: SelfJwtValidator[F]): F[Response[F]] = {
    val result = for {
      jwt <- EitherT.fromEither[F](JwtExtractor.extractJwt(req).toRight(JwtNotFound))
      ahu <- selfJwtAuthenticator.authenticate(jwt)
    } yield f(ahu)

    result.value.flatMap {
      case Left(e) =>
        Response[F](Status.Unauthorized).withBody(s"JWT validation failed: ${e.message}")
      case Right(r) => r
    }
  }
}
