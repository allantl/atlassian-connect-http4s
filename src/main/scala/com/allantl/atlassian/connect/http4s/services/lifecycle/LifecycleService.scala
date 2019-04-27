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

class LifecycleService[F[_]: Monad](hostRepo: AtlassianHostRepositoryAlgebra[F], infoLogger: String => F[Unit]) {

  def install(installEvent: InstallEvent): F[Unit] =
    for {
      maybeAtlassianHost <- hostRepo.findByClientKey(installEvent.clientKey, onlyInstalled = false)
      record <- maybeAtlassianHost match {
        case Some(existing) =>
          val logInstallEvent =
            if (existing.baseUrl != installEvent.baseUrl) {
              infoLogger(
                s"Updating baseUrl from ${existing.baseUrl} to ${installEvent.baseUrl} for host $existing")
            } else {
              infoLogger(
                s"Reinstalling addon with baseUrl ${existing.baseUrl} for host $existing")
            }
          logInstallEvent *> newInstallationRecord(installEvent).pure[F]
        case None =>
          newInstallationRecord(installEvent).pure[F]
      }
      host <- hostRepo.save(record)
      _ <- infoLogger(s"Successfully installed host $host from payload: $installEvent")
    } yield ()

  def uninstall(uninstallEvent: UninstallEvent): EitherT[F, HostError, Unit] =
    for {
      host <- EitherT(
        hostRepo.findByClientKey(uninstallEvent.clientKey).map(_.toRight(HostNotFound: HostError))
      )
      _ <- EitherT.liftF(hostRepo.save(host.copy(installed = false)))
      _ <- EitherT.liftF(
        infoLogger(s"Successfully uninstalled host $host from payload: $uninstallEvent")
      )
    } yield ()
}

object LifecycleService {

  def apply[F[_]: Monad](hostRepo: AtlassianHostRepositoryAlgebra[F], infoLogger: String => F[Unit]) =
    new LifecycleService[F](hostRepo, infoLogger)
}
