package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.actions

import cats.Monad
import cats.data.EitherT
import cats.syntax.flatMap._
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.errors.JwtNotFound
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.{JwtAuthenticator, JwtExtractor}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import org.http4s.{Request, Response, Status}

/**
  * Atlassian Connect Jwt Authentication
  *
  */
object AcJwtAuthenticated {

  def apply[F[_]: Monad](req: Request[F])(f: AtlassianHostUser => F[Response[F]])(
      implicit jwtAuthenticator: JwtAuthenticator[F]): F[Response[F]] = {
    val result = for {
      jwt <- EitherT.fromEither[F](JwtExtractor.extractJwt(req).toRight(JwtNotFound))
      ahu <- jwtAuthenticator.authenticate(jwt)
    } yield f(ahu)

    result.value.flatMap {
      case Left(e) => Response[F](Status.Unauthorized).withBody(s"JWT validation failed: ${e.message}")
      case Right(r) => r
    }
  }
}