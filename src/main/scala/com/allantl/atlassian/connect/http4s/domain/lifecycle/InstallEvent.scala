package com.allantl.atlassian.connect.http4s.domain.lifecycle

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class InstallEvent(
    eventType: String,
    key: String,
    clientKey: String,
    publicKey: String,
    oauthClientId: Option[String],
    sharedSecret: String,
    serverVersion: String,
    pluginsVersion: String,
    baseUrl: String,
    productType: String,
    description: String,
    serviceEntitlementNumber: Option[String])

object InstallEvent {
  implicit val encoder: Encoder[InstallEvent] = deriveEncoder
  implicit val decoder: Decoder[InstallEvent] = deriveDecoder
}
