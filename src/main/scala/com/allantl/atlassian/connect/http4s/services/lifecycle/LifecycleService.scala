package com.allantl.atlassian.connect.http4s.services.lifecycle

import cats.Monad
import cats.data.EitherT
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.bifunctor._
import cats.instances.either._
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost.newInstallationRecord
import com.allantl.atlassian.connect.http4s.domain.{
  AtlassianHostUser,
  HostError,
  HostNotFound
}
import com.allantl.atlassian.connect.http4s.domain.lifecycle.{InstallEvent, UninstallEvent}
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra

class LifecycleService[F[_]: Monad](
    hostRepo: AtlassianHostRepositoryAlgebra[F],
    infoLogger: String => F[Unit]
) {

  def install(installEvent: InstallEvent): F[Unit] =
    for {
      host <- hostRepo.save(newInstallationRecord(installEvent))
      _ <- infoLogger(s"Successfully installed host $host from payload: $installEvent")
    } yield ()

  def reinstall(installEvent: InstallEvent, ahu: AtlassianHostUser): F[Unit] = {
    val host = ahu.host
    val logInstallEvent = if (host.baseUrl != installEvent.baseUrl) {
      infoLogger(s"Updating baseUrl from ${host.baseUrl} to ${installEvent.baseUrl} for host $host")
    } else {
      infoLogger(s"Reinstalling addon with baseUrl ${host.baseUrl} for host $host")
    }

    for {
      _ <- logInstallEvent
      updatedHost <- hostRepo.save(newInstallationRecord(installEvent))
      _ <- infoLogger(s"Successfully reinstalled host $updatedHost from payload: $installEvent")
    } yield ()
  }

  def uninstall(uninstallEvent: UninstallEvent): EitherT[F, HostError, Unit] =
    for {
      host <- EitherT(
        hostRepo
          .findByClientKey(uninstallEvent.clientKey)
          .map(_.toRight(HostNotFound).leftWiden[HostError])
      )
      _ <- EitherT.liftF(hostRepo.save(host.copy(installed = false)))
      _ <- EitherT.liftF(
        infoLogger(s"Successfully uninstalled host $host from payload: $uninstallEvent")
      )
    } yield ()
}

object LifecycleService {

  def apply[F[_]: Monad](
      hostRepo: AtlassianHostRepositoryAlgebra[F],
      infoLogger: String => F[Unit]) =
    new LifecycleService[F](hostRepo, infoLogger)
}
