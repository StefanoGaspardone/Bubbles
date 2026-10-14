package com.backend.models.projections

import java.util.UUID

interface DiveNumberProjection {
    val id: UUID
    val diveNumber: Long
}