package com.allantl.atlassian.connect.http4s.auth.domain

import com.allantl.atlassian.connect.http4s.domain.AtlassianHostUser
import org.http4s.Request

final case class AcJwtAuthenticatedRequest[F[_]](
    request: Request[F],
    ahu: AtlassianHostUser
)
