package com.allantl.atlassian.connect.http4s

import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import cats.implicits._
import com.allantl.atlassian.connect.http4s.auth.domain.AcJwtAuthenticatedRequest
import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import org.http4s.server.Middleware
import org.http4s.{Request, Response}

package object auth {

  type AcHttpRoutes[F[_]] = Kleisli[OptionT[F, ?], AcJwtAuthenticatedRequest[F], Response[F]]
  type AcHttpMiddleware[F[_]] =
    Middleware[OptionT[F, ?], AcJwtAuthenticatedRequest[F], Response[F], Request[F], Response[F]]

  object AcHttpRoutes {

    def of[F[_]](
        pf: PartialFunction[AcJwtAuthenticatedRequest[F], F[Response[F]]]
    )(implicit F: Sync[F]): AcHttpRoutes[F] =
      Kleisli(req => OptionT(F.suspend(pf.lift(req).sequence)))
  }

  object asAcAuth {
    def unapply[F[_]](ac: AcJwtAuthenticatedRequest[F]): Option[(Request[F], AtlassianHostUser)] =
      Some(ac.request -> ac.ahu)
  }
}
