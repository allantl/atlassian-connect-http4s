package com.allantl.atlassian.connect.http4s.endpoints

import cats.effect.Effect
import org.http4s.{EntityDecoder, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import com.allantl.atlassian.connect.http4s.domain.lifecycle.{InstallEvent, UninstallEvent}
import cats.syntax.functor._
import cats.syntax.flatMap._
import com.allantl.atlassian.connect.http4s.auth.{AcHttpRoutes, asAcAuth}
import com.allantl.atlassian.connect.http4s.services.lifecycle.LifecycleService
import org.http4s.circe._
import cats.syntax.semigroupk._
import com.allantl.atlassian.connect.http4s.auth.middleware.AcHttpService

class LifecycleEndpoints[F[_]: Effect](acHttpService: AcHttpService[F], lifecycleService: LifecycleService[F])
    extends Http4sDsl[F] {

  implicit val installedDecoder: EntityDecoder[F, InstallEvent] = jsonOf[F, InstallEvent]
  implicit val uninstalledDecoder: EntityDecoder[F, UninstallEvent] = jsonOf[F, UninstallEvent]

  private val authEndpoints: AcHttpRoutes[F] = AcHttpRoutes.of[F] {
    case req @ POST -> Root / "uninstalled" asAcAuth _ =>
      for {
        uninstallEvent <- req.request.as[UninstallEvent]
        errOrSuccess <- lifecycleService.uninstall(uninstallEvent).value
        response <- errOrSuccess.fold(
          _ => NotFound(s"Unable to uninstall addon from ${uninstallEvent.baseUrl}"),
          _ => Ok()
        )
      } yield response
  }

  private val noAuthEndpoints: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req @ POST -> Root / "installed" =>
        for {
          installEvent <- req.as[InstallEvent]
          _ <- lifecycleService.install(installEvent)
          ok <- Ok()
        } yield ok
    }

  val endpoints: HttpRoutes[F] =
    acHttpService.liftRoutes(authEndpoints) <+> noAuthEndpoints
}
