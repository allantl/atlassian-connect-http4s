package com.allantl.atlassian.connect.http4s.endpoints

import cats.data.EitherT
import cats.effect.ConcurrentEffect
import cats.effect.syntax.concurrent._
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.{JwtExtractor, JwtValidator}
import com.allantl.atlassian.connect.http4s.auth.errors.{JwtAuthenticationError, JwtNotFound}
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import com.allantl.atlassian.connect.http4s.domain.lifecycle.{InstallEvent, UninstallEvent}
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra
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
    hostRepo: AtlassianHostRepositoryAlgebra[F],
    lifecycleService: LifecycleService[F],
    lifecycleEventHandler: LifecycleEventHandler[F]
) extends Http4sDsl[F] {

  implicit val installedDecoder: EntityDecoder[F, InstallEvent] = jsonOf[F, InstallEvent]
  implicit val uninstalledDecoder: EntityDecoder[F, UninstallEvent] = jsonOf[F, UninstallEvent]

  val endpoints: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "installed" =>
        req.as[InstallEvent].flatMap { installEvent =>
          if (installEvent.clientKey.isEmpty) BadRequest("Invalid install event")
          else {
            // Check whether addon is already installed
            // If yes, we need to verify JWT
            hostRepo.findByClientKey(installEvent.clientKey).flatMap {
              case Some(_) =>
                authenticateJwt(req).flatMap {
                  case Left(_) =>
                    Response[F](Unauthorized).pure[F]

                  case Right(ahu) =>
                    for {
                      _ <- lifecycleService.reinstall(installEvent, ahu)
                      _ <- lifecycleEventHandler.afterInstall().start.void
                      ok <- Ok()
                    } yield ok
                }

              case None =>
                for {
                  _ <- lifecycleService.install(installEvent)
                  _ <- lifecycleEventHandler.afterInstall().start.void
                  ok <- Ok()
                } yield ok
            }
          }
        }

      case req @ POST -> Root / "uninstalled" =>
        authenticateJwt(req).flatMap {
          case Left(_) =>
            Response[F](Status.Unauthorized).pure[F]

          case Right(_) =>
            for {
              uninstallEvent <- req.as[UninstallEvent]
              errOrSuccess <- lifecycleService.uninstall(uninstallEvent).value
              response <- errOrSuccess.fold(
                _ => NotFound(s"Unable to uninstall addon from ${uninstallEvent.baseUrl} due to invalid JWT"),
                _ => lifecycleEventHandler.afterUninstall().start.void >> Ok()
              )
            } yield response
        }
    }

  private def authenticateJwt(
      req: Request[F]
  ): F[Either[JwtAuthenticationError, AtlassianHostUser]] =
    (for {
      jwt <- EitherT.fromEither[F](JwtExtractor.extractJwt(req).toRight(JwtNotFound))
      ahu <- EitherT(jwtValidator.authenticate(jwt))
    } yield ahu).value
}

object LifecycleEndpoints {

  def apply[F[_]: ConcurrentEffect](
      jwtValidator: JwtValidator[F],
      hostRepo: AtlassianHostRepositoryAlgebra[F],
      lifecycleService: LifecycleService[F]
  ): LifecycleEndpoints[F] =
    new LifecycleEndpoints[F](
      jwtValidator,
      hostRepo,
      lifecycleService,
      new NoOpLifecycleEventHandler[F]()) {}

  def apply[F[_]: ConcurrentEffect](
      jwtValidator: JwtValidator[F],
      hostRepo: AtlassianHostRepositoryAlgebra[F],
      lifecycleService: LifecycleService[F],
      lifecycleEventHandler: LifecycleEventHandler[F]
  ): LifecycleEndpoints[F] =
    new LifecycleEndpoints[F](jwtValidator, hostRepo, lifecycleService, lifecycleEventHandler) {}
}
