package com.allantl.atlassian.connect.http4s.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class AtlassianConnectContext(
    user: UserContext
)

object AtlassianConnectContext {
  implicit val decoder: Decoder[AtlassianConnectContext] = deriveDecoder
  implicit val encoder: Encoder[AtlassianConnectContext] = deriveEncoder
}

case class UserContext(
    accountId: String,
    userKey: String,
    username: String,
    displayName: String
)

object UserContext {
  implicit val decoder: Decoder[UserContext] = deriveDecoder
  implicit val encoder: Encoder[UserContext] = deriveEncoder
}
