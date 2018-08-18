package com.allantl.atlassian.connect.http4s.services.lifecycle

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.apply._
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost.newInstallationRecord
import com.allantl.atlassian.connect.http4s.domain.{HostError, HostNotFound}
import com.allantl.atlassian.connect.http4s.domain.lifecycle.{InstallEvent, UninstallEvent}
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra
import io.chrisdavenport.log4cats.Logger

class LifecycleService[F[_]: Monad: Logger](hostRepo: AtlassianHostRepositoryAlgebra[F]) {

  def install(installEvent: InstallEvent): F[Unit] =
    for {
      maybeAtlassianHost <- hostRepo.findByClientKey(installEvent.clientKey, onlyInstalled = false)
      record <- maybeAtlassianHost match {
        case Some(existing) =>
          val logInstallEvent =
            if (existing.baseUrl != installEvent.baseUrl) {
              Logger[F].warn(
                s"Updating baseUrl from ${existing.baseUrl} to ${installEvent.baseUrl} for host $existing")
            } else {
              Logger[F].info(
                s"Reinstalling addon with baseUrl ${existing.baseUrl} for host $existing")
            }
          logInstallEvent *> newInstallationRecord(installEvent).pure[F]
        case None =>
          newInstallationRecord(installEvent).pure[F]
      }
      host <- hostRepo.save(record)
      _ = Logger[F].info(s"Successfully installed host $host from payload: $installEvent")
    } yield ()

  def uninstall(uninstallEvent: UninstallEvent): EitherT[F, HostError, Unit] =
    for {
      host <- EitherT(
        hostRepo.findByClientKey(uninstallEvent.clientKey).map(_.toRight(HostNotFound: HostError)))
      _ <- EitherT.liftF(hostRepo.save(host.copy(installed = false)))
      _ = Logger[F].info(s"Successfully uninstalled host $host from payload: $uninstallEvent")
    } yield ()
}
