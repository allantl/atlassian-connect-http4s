package com.allantl.atlassian.connect.http4s.middleware

import cats.Functor
import cats.data.{Kleisli, OptionT}
import com.allantl.atlassian.connect.config.AtlassianConnectConfig
import org.http4s._

final class LicenseCheck[F[_]: Functor](ifUnlicensed: Request[F] => F[Response[F]])(
    implicit acConfig: AtlassianConnectConfig
) {

  def apply(service: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      (acConfig.licenseCheckEnabled, req.params.get("lic")) match {
        case (false, _) => service(req)
        case (true, Some("active")) => service(req)
        case _ => OptionT.liftF(ifUnlicensed(req))
      }
    }
}

object LicenseCheck {

  def apply[F[_]](service: HttpRoutes[F])(implicit instance: LicenseCheck[F]): HttpRoutes[F] =
    instance.apply(service)
}
