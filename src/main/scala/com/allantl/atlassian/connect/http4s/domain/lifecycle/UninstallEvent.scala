package com.allantl.atlassian.connect.http4s.domain.lifecycle

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class UninstallEvent(eventType: String,
                          key: String,
                          clientKey: String,
                          publicKey: String,
                          oauthClientId: Option[String],
                          serverVersion: String,
                          pluginsVersion: String,
                          baseUrl: String,
                          productType: String,
                          description: String,
                          serviceEntitlementNumber: Option[String])

object UninstallEvent {
  implicit val encoder: Encoder[UninstallEvent] = deriveEncoder
  implicit val decoder: Decoder[UninstallEvent] = deriveDecoder
}
