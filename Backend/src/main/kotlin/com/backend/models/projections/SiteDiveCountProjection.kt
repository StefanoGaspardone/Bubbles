package com.backend.models.projections

import java.util.UUID

interface SiteDiveCountProjection {
    val siteId: UUID
    val diveCount: Long
}