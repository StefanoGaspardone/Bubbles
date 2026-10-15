package com.backend.models.dtos

import com.backend.models.entities.Dive
import com.backend.models.entities.DiveBuddy
import com.backend.models.enums.BuddyConfirmationStatus
import com.backend.models.enums.BuddyRole
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Schema(
    description = "Buddy of the dive. Use exactly one form: diveBuddyId (keep an existing entry with its confirmation, only on update), userId (real user, confirmation via QR) or firstName/lastName (custom buddy without account)."
)
data class DiveBuddySelectionDTO(
    @field:Schema(description = "Id of a buddy entry already saved in this dive", nullable = true)
    val diveBuddyId: UUID? = null,

    @field:Schema(description = "Id of a real user (from GET /users/lookup)", nullable = true)
    val userId: UUID? = null,

    @field:Schema(description = "First name of a custom buddy", example = "Luca", nullable = true)
    @field:Size(max = 50)
    @field:Pattern(regexp = ".*\\S.*", message = "First name must not be blank")
    val firstName: String? = null,

    @field:Schema(description = "Last name of a custom buddy (optional)", example = "Bianchi", nullable = true)
    @field:Size(max = 50)
    val lastName: String? = null,

    @field:Schema(description = "Role of the buddy in the dive. Also applied when keeping an existing entry.", example = "BUDDY")
    val role: BuddyRole = BuddyRole.BUDDY,
) {
    @get:JsonIgnore
    @get:Schema(hidden = true)
    @get:AssertTrue(message = "use exactly one of diveBuddyId, userId or firstName")
    val isValidSelection: Boolean
        get() = listOf(diveBuddyId != null, userId != null, firstName != null).count { it } == 1

    @get:JsonIgnore
    @get:Schema(hidden = true)
    @get:AssertTrue(message = "lastName is allowed only for custom buddies")
    val isLastNameAllowed: Boolean
        get() = lastName == null || firstName != null
}

@Schema(description = "Buddy of a dive, with confirmation status")
data class DiveBuddyDTO(
    @field:Schema(description = "Id of this buddy entry (use it as diveBuddyId to keep it on update)")
    val id: UUID,

    @field:Schema(description = "Id of the real user. Null for custom buddies.", nullable = true)
    val userId: UUID?,

    @field:Schema(description = "Current username of the real user. Null for custom buddies.", example = "marco.sub", nullable = true)
    val username: String?,

    @field:Schema(
        description = "Current avatar file id of the real user. Null for custom buddies and for users without a custom avatar.",
        nullable = true,
    )
    val avatarId: UUID?,

    @field:Schema(description = "First name (for real users: as it was when added)", example = "Marco")
    val firstName: String,

    @field:Schema(description = "Last name", example = "Rossi", nullable = true)
    val lastName: String?,

    @field:Schema(description = "Role of the buddy in the dive", example = "BUDDY")
    val role: BuddyRole,

    @field:Schema(description = "NOT_REQUIRED for custom buddies; PENDING or CONFIRMED for real users", example = "PENDING")
    val confirmationStatus: BuddyConfirmationStatus,

    @field:Schema(description = "When the buddy confirmed the dive", nullable = true)
    val confirmedAt: Instant?,
) {
    companion object {
        fun from(buddy: DiveBuddy) = DiveBuddyDTO(
            id = buddy.id!!,
            userId = buddy.buddyUser?.id,
            username = buddy.buddyUser?.username,
            avatarId = buddy.buddyUser?.avatarId,
            firstName = buddy.firstName,
            lastName = buddy.lastName,
            role = buddy.role,
            confirmationStatus = buddy.confirmationStatus,
            confirmedAt = buddy.confirmedAt,
        )
    }
}

@Schema(description = "Summary of the dive just confirmed by the current user as buddy")
data class DiveConfirmationDTO(
    @field:Schema(description = "Dive id")
    val diveId: UUID,

    @field:Schema(description = "Name of the diver who logged the dive", example = "Stefano Rossi")
    val loggedBy: String,

    @field:Schema(description = "Dive site name", example = "Secca dell'Isuela")
    val siteName: String,

    @field:Schema(description = "Local date and time of entry at the dive site", example = "2026-08-10T09:30:00")
    val startLocal: LocalDateTime,

    @field:Schema(description = "Total dive time in minutes", example = "52")
    val durationMinutes: Long,

    @field:Schema(description = "Maximum depth in meters", example = "24.5")
    val maxDepthM: Double,

    @field:Schema(description = "Role of the current user in the dive", example = "BUDDY")
    val role: BuddyRole,

    @field:Schema(description = "When the current user confirmed the dive")
    val confirmedAt: Instant,
) {
    companion object {
        fun from(dive: Dive, buddy: DiveBuddy) = DiveConfirmationDTO(
            diveId = dive.id!!,
            loggedBy = "${dive.owner.firstName} ${dive.owner.lastName}",
            siteName = dive.site.name,
            startLocal = dive.localStart().toLocalDateTime(),
            durationMinutes = dive.duration().toMinutes(),
            maxDepthM = dive.maxDepthM,
            role = buddy.role,
            confirmedAt = buddy.confirmedAt!!,
        )
    }
}