package com.allantl.atlassian.connect.http4s.endpoints

import cats.effect.Effect
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import com.allantl.atlassian.connect.http4s.domain.lifecycle.{InstallEvent, UninstallEvent}
import cats.syntax.functor._
import cats.syntax.flatMap._
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.JwtValidator
import com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.actions.JwtAuthenticated
import com.allantl.atlassian.connect.http4s.services.lifecycle.LifecycleService
import org.http4s.circe._

class LifecycleEndpoints[F[_]: Effect: JwtValidator](lifecycleService: LifecycleService[F])
    extends Http4sDsl[F] {

  implicit val installedDecoder: EntityDecoder[F, InstallEvent] = jsonOf[F, InstallEvent]
  implicit val uninstalledDecoder: EntityDecoder[F, UninstallEvent] = jsonOf[F, UninstallEvent]

  val endpoints: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "installed" =>
        for {
          installEvent <- req.as[InstallEvent]
          _ <- lifecycleService.install(installEvent)
          ok <- Ok()
        } yield ok

      case req @ POST -> Root / "uninstalled" =>
        JwtAuthenticated(req) { _ =>
          for {
            uninstallEvent <- req.as[UninstallEvent]
            errOrSuccess <- lifecycleService.uninstall(uninstallEvent).value
            response <- errOrSuccess.fold(
              _ => NotFound(s"Unable to uninstall addon from ${uninstallEvent.baseUrl}"),
              _ => Ok()
            )
          } yield response
        }
    }
}
