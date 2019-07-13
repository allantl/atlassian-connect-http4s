package com.allantl.atlassian.connect.http4s.endpoints

import cats.data.EitherT
import cats.effect.ConcurrentEffect
import cats.effect.syntax.concurrent._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.{JwtExtractor, JwtValidator}
import com.allantl.atlassian.connect.http4s.auth.errors.JwtNotFound
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import com.allantl.atlassian.connect.http4s.domain.lifecycle.{InstallEvent, UninstallEvent}
import com.allantl.atlassian.connect.http4s.services.lifecycle.{
  LifecycleEventHandler,
  LifecycleService,
  NoOpLifecycleEventHandler
}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes, Request, Response, Status}

sealed abstract class LifecycleEndpoints[F[_]: ConcurrentEffect](
    jwtValidator: JwtValidator[F],
    lifecycleService: LifecycleService[F],
    lifecycleEventHandler: LifecycleEventHandler[F]
) extends Http4sDsl[F] {

  implicit val installedDecoder: EntityDecoder[F, InstallEvent] = jsonOf[F, InstallEvent]
  implicit val uninstalledDecoder: EntityDecoder[F, UninstallEvent] = jsonOf[F, UninstallEvent]

  val endpoints: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "installed" =>
        for {
          installEvent <- req.as[InstallEvent]
          _ <- lifecycleService.install(installEvent)
          _ <- lifecycleEventHandler.afterInstall().start.void
          ok <- Ok()
        } yield ok

      case req @ POST -> Root / "uninstalled" =>
        jwtAuthenticated(req) { _ =>
          for {
            uninstallEvent <- req.as[UninstallEvent]
            errOrSuccess <- lifecycleService.uninstall(uninstallEvent).value
            _ <- lifecycleEventHandler.afterUninstall().start.void
            response <- errOrSuccess.fold(
              _ => NotFound(s"Unable to uninstall addon from ${uninstallEvent.baseUrl}"),
              _ => Ok()
            )
          } yield response
        }
    }

  private def jwtAuthenticated(req: Request[F])(
      f: AtlassianHostUser => F[Response[F]]
  ): F[Response[F]] = {
    val res = for {
      jwt <- EitherT.fromEither[F](JwtExtractor.extractJwt(req).toRight(JwtNotFound))
      ahu <- EitherT(jwtValidator.authenticate(jwt))
    } yield f(ahu)

    res.getOrElse(Response[F](Status.Unauthorized).pure[F]).flatten
  }
}

object LifecycleEndpoints {

  def apply[F[_]: ConcurrentEffect](
      jwtValidator: JwtValidator[F],
      lifecycleService: LifecycleService[F]
  ): LifecycleEndpoints[F] =
    new LifecycleEndpoints[F](jwtValidator, lifecycleService, new NoOpLifecycleEventHandler[F]()) {}

  def apply[F[_]: ConcurrentEffect](
      jwtValidator: JwtValidator[F],
      lifecycleService: LifecycleService[F],
      lifecycleEventHandler: LifecycleEventHandler[F]
  ): LifecycleEndpoints[F] =
    new LifecycleEndpoints[F](jwtValidator, lifecycleService, lifecycleEventHandler) {}
}
