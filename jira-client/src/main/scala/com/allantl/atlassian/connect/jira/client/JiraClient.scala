package com.allantl.atlassian.connect.jira.client

import com.allantl.atlassian.connect.config.{AddOnProperties, AtlassianConnectConfig}
import com.allantl.jira4s.auth.AcJwtConfig
import com.allantl.jira4s.v2.JiraMultiTenantClient
import com.softwaremill.sttp.SttpBackend

object JiraClient {

  type JiraClient[R[_]] = JiraMultiTenantClient[R]

  def apply[R[_], S](acConfig: AtlassianConnectConfig, addOnProperties: AddOnProperties)(
      implicit sttpBackend: SttpBackend[R, S]
  ): JiraMultiTenantClient[R] =
    JiraMultiTenantClient(
      AcJwtConfig(
        addOnProperties.key,
        acConfig.jwtExpirationTimeInSeconds
      )
    )
}
