package com.allantl.atlassian.connect.http4s.middleware

import cats.Functor
import org.http4s._
import cats.data.Kleisli
import com.allantl.atlassian.connect.http4s.configs.AtlassianConnectConfig

class LicenseCheck[F[_]: Functor](ifUnlicensed: HttpService[F])(implicit acConfig: AtlassianConnectConfig) {

  def apply(service: HttpService[F]): HttpService[F] =
    Kleisli { req: Request[F] =>
      (acConfig.licenseCheck, req.params.get("lic")) match {
        case (false, _) => service(req)
        case (true, Some("active")) => service(req)
        case _ => ifUnlicensed(req)
      }
    }
}

object LicenseCheck {

  def apply[F[_]](service: HttpService[F])(implicit instance: LicenseCheck[F]): HttpService[F] =
    instance.apply(service)
}
