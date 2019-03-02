package com.allantl.atlassian.connect.http4s.domain

import com.allantl.atlassian.connect.http4s.domain.lifecycle.InstallEvent

case class AtlassianHost(
    clientKey: String,
    key: String,
    publicKey: String,
    oauthClientId: Option[String],
    sharedSecret: String,
    severVersion: String,
    pluginsVersion: String,
    baseUrl: String,
    productType: String,
    description: String,
    serviceEntitlementNumber: Option[String],
    installed: Boolean)

object AtlassianHost {

  def newInstallationRecord(installEvent: InstallEvent): AtlassianHost =
    AtlassianHost(
      installEvent.clientKey,
      installEvent.key,
      installEvent.publicKey,
      installEvent.oauthClientId,
      installEvent.sharedSecret,
      installEvent.serverVersion,
      installEvent.pluginsVersion,
      installEvent.baseUrl,
      installEvent.productType,
      installEvent.description,
      installEvent.serviceEntitlementNumber,
      installed = true
    )
}
