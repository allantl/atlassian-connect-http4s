package com.allantl.atlassian.connect.http4s.auth.atlassian.jwt.domain

import io.toolsplus.atlassian.jwt.api.CanonicalHttpRequest

case class JwtCredentials(rawJwt: String, canonicalHttpRequest: CanonicalHttpRequest)
