package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.actions

import cats.Monad
import cats.data.EitherT
import cats.syntax.flatMap._
import cats.syntax.applicative._
import com.allantl.atlassian.connect.http4s.auth.errors.JwtNotFound
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.{JwtExtractor, JwtValidator}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import org.http4s.{Request, Response, Status}

/**
  * Atlassian Connect Jwt Authentication
  */
object JwtAuthenticated {

  def apply[F[_]: Monad](req: Request[F])(f: AtlassianHostUser => F[Response[F]])(
      implicit jwtValidator: JwtValidator[F]): F[Response[F]] = {
    val result = for {
      jwt <- EitherT.fromEither[F](JwtExtractor.extractJwt(req).toRight(JwtNotFound))
      ahu <- jwtValidator.authenticate(jwt)
    } yield f(ahu)

    result.value.flatMap {
      case Left(e) =>
        Response[F](Status.Unauthorized).withEntity(s"JWT validation failed: ${e.message}").pure[F]
      case Right(r) => r
    }
  }
}
