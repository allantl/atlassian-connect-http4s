package com.allantl.atlassian.connect.http4s.repository.algebra

import com.allantl.atlassian.connect.http4s.domain.AtlassianHost

trait AtlassianHostRepositoryAlgebra[F[_]] {

  def findByClientKey(clientKey: String, onlyInstalled: Boolean = true): F[Option[AtlassianHost]]

  def findByBaseUrl(baseUrl: String, onlyInstalled: Boolean = true): F[Option[AtlassianHost]]

  def save(atlassianHost: AtlassianHost): F[AtlassianHost]
}
