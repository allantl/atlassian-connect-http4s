package com.allantl.atlassian.connect.http4s.auth.middleware

import cats.MonadError
import cats.data.{EitherT, Kleisli, OptionT}
import com.allantl.atlassian.connect.http4s.auth.{AcHttpMiddleware, AcHttpRoutes}
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.{JwtExtractor, JwtValidator}
import org.http4s.{HttpRoutes, Request, Response, Status}
import cats.syntax.applicative._
import com.allantl.atlassian.connect.http4s.auth.domain.AcJwtAuthenticatedRequest

sealed abstract class AcHttpService[F[_]](jwtValidator: JwtValidator[F])(
    implicit F: MonadError[F, Throwable],
    ME: MonadError[Kleisli[OptionT[F, ?], Request[F], ?], Throwable]
) {

  private val defaultUnauthorized: Request[F] => F[Response[F]] =
    _ => Response[F](Status.Unauthorized).pure[F]

  def liftRoutes(
      acHttpRoutes: AcHttpRoutes[F],
      ifUnauthorized: Request[F] => F[Response[F]] = defaultUnauthorized
  ): HttpRoutes[F] =
    ME.handleErrorWith(handle(ifUnauthorized)(acHttpRoutes)) { _: Throwable =>
      Kleisli.liftF(OptionT.pure(Response[F](Status.Unauthorized)))
    }

  private def handle(
      ifUnauthorized: Request[F] => F[Response[F]]
  ): AcHttpMiddleware[F] = { service =>
    Kleisli { r: Request[F] =>
      Kleisli(authenticate)
        .andThen(service.mapF(o => OptionT.liftF(o.getOrElse(Response[F](Status.NotFound)))))
        .mapF(o => OptionT.liftF(o.getOrElseF(ifUnauthorized(r))))
        .run(r)
    }
  }

  private def authenticate(
      req: Request[F]
  ): OptionT[F, AcJwtAuthenticatedRequest[F]] =
    for {
      jwt <- OptionT.fromOption[F](JwtExtractor.extractJwt(req))
      ahu <- EitherT(jwtValidator.authenticate(jwt)).toOption
    } yield AcJwtAuthenticatedRequest[F](req, ahu)
}

object AcHttpService {

  def apply[F[_]](jwtValidator: JwtValidator[F])(
      implicit F: MonadError[F, Throwable],
      ME: MonadError[Kleisli[OptionT[F, ?], Request[F], ?], Throwable]
  ): AcHttpService[F] =
    new AcHttpService[F](jwtValidator) {}
}
