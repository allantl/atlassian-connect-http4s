package com.allantl.atlassian.connect.http4s.mock.repository

import cats.Applicative
import com.allantl.atlassian.connect.http4s.domain.AtlassianHost
import com.allantl.atlassian.connect.http4s.repository.algebra.AtlassianHostRepositoryAlgebra
import cats.syntax.applicative._
import cats.syntax.option._

class TestAtlassianHostRepository[F[_]: Applicative](implicit host: AtlassianHost)
    extends AtlassianHostRepositoryAlgebra[F] {

  override def findByClientKey(clientKey: String, onlyInstalled: Boolean): F[Option[AtlassianHost]] =
    Option(host).pure[F]

  override def findByBaseUrl(baseUrl: String, onlyInstalled: Boolean): F[Option[AtlassianHost]] =
    Option(host).pure[F]

  override def save(atlassianHost: AtlassianHost): F[AtlassianHost] =
    host.pure[F]
}

class NotFoundHostRepository[F[_]: Applicative] extends AtlassianHostRepositoryAlgebra[F] {

  override def findByClientKey(clientKey: String, onlyInstalled: Boolean): F[Option[AtlassianHost]] =
    none[AtlassianHost].pure[F]

  override def findByBaseUrl(baseUrl: String, onlyInstalled: Boolean): F[Option[AtlassianHost]] =
    none[AtlassianHost].pure[F]

  override def save(atlassianHost: AtlassianHost): F[AtlassianHost] =
    atlassianHost.pure[F]
}
