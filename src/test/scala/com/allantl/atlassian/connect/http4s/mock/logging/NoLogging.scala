package com.allantl.atlassian.connect.http4s.mock.logging

import cats.Applicative
import io.chrisdavenport.log4cats.Logger

class NoLogging[F[_]: Applicative] extends Logger[F] {
  override def error(message: => String) =
    Applicative[F].pure(())

  override def error(t: Throwable)(message: => String) =
    Applicative[F].pure(())

  override def warn(message: => String) =
    Applicative[F].pure(())

  override def warn(t: Throwable)(message: => String) =
    Applicative[F].pure(())

  override def info(message: => String) =
    Applicative[F].pure(())

  override def info(t: Throwable)(message: => String) =
    Applicative[F].pure(())

  override def debug(message: => String) =
    Applicative[F].pure(())

  override def debug(t: Throwable)(message: => String) =
    Applicative[F].pure(())

  override def trace(message: => String) =
    Applicative[F].pure(())

  override def trace(t: Throwable)(message: => String) =
    Applicative[F].pure(())
}

object NoLogging {

  def apply[F[_]: Applicative](): NoLogging[F] = new NoLogging[F]()

}
