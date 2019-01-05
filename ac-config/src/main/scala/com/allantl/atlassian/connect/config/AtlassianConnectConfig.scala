package com.allantl.atlassian.connect.config

case class AtlassianConnectConfig(
    jwtExpirationTimeInSeconds: Long,
    licenseCheckEnabled: Boolean
)
