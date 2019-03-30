package com.allantl.atlassian.connect.http4s.services.lifecycle

import cats.Applicative
import cats.syntax.applicative._

trait LifecycleEventHandler[F[_]] {

  def afterInstall(): F[Unit]

  def afterUninstall(): F[Unit]
}

class NoOpLifecycleEventHandler[F[_]: Applicative] extends LifecycleEventHandler[F] {

  override def afterInstall(): F[Unit] = ().pure[F]

  override def afterUninstall(): F[Unit] = ().pure[F]
}
