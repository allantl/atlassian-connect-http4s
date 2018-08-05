package com.allantl.atlassian.connect.http4s.auth.self.jwt.actions
import cats.Monad
import com.allantl.atlassian.connect.http4s.auth.self.jwt.{SelfJwtGenerator, SelfJwtValidator}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import org.http4s.{Request, Response}

/**
  * Authenticate incoming request with JWT.
  * A JWT Token Header will be added to the outgoing response.
  *
  */
object SelfJwtHandler {

  def apply[F[_]: Monad: SelfJwtValidator](req: Request[F])(f: AtlassianHostUser => F[Response[F]])(
      implicit selfJwtGenerator: SelfJwtGenerator): F[Response[F]] =
    SelfJwtAuthenticated(req) { implicit ahu =>
      WithSelfJwtHeader(f(ahu))
    }
}
