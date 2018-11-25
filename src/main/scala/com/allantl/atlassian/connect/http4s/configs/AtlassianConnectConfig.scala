package com.allantl.atlassian.connect.http4s.configs

case class AtlassianConnectConfig(
    jwtExpirationTimeInSeconds: Long,
    licenseCheckEnabled: Boolean
)
