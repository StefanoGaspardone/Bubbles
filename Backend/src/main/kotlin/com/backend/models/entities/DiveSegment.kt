package com.backend.models.entities

import com.backend.models.enums.DiveSegmentType

data class DiveSegment(
    val type: DiveSegmentType = DiveSegmentType.BOTTOM,
    val depthM: Double = 0.0,
    val durationSeconds: Int = 0,
)