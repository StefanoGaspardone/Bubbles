package com.backend.models.projections

import com.backend.models.enums.AccountStatus
import com.backend.models.enums.UserRole

interface UserAuthProjection {
    val accountStatus: AccountStatus
    val role: UserRole
}