package com.backend.models.dtos

import com.backend.models.entities.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "Representation of user")
data class UserDTO(
    @field:Schema(description = "Unique identifier of the user", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    val id: UUID,

    @field:Schema(description = "Username, always lowercase", example = "stivigas")
    val username: String,

    @field:Schema(description = "Email address, always lowercase", example = "user@example.com")
    val email: String,

    @field:Schema(description = "First name", example = "John")
    val firstName: String,

    @field:Schema(description = "Last name", example = "Doe")
    val lastName: String,

    @field:Schema(description = "Role assigned to the user", example = "USER")
    val role: String,

    @field:Schema(description = "ID of the uploaded avatar file. Null if no custom avatar was uploaded.")
    val avatarId: UUID?,

    @field:Schema(description = "Whether the user has push/email notifications enabled", example = "true")
    val notificationsEnabled: Boolean,
) {
    companion object {
        fun from(user: User) = UserDTO(
            id = user.id!!,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role.name,
            avatarId = user.avatarId,
            notificationsEnabled = user.notificationsEnabled,
        )
    }
}

/**
 * Partial update: null fields are not modified.
 * Bean Validation constraints ignore null values, so they apply only to the fields actually sent.
 */
@Schema(description = "Payload to update the current user's profile. Fields left null are not modified.")
data class UpdateUserRequestDTO(
    @field:Schema(
        description = "New username: 3-50 characters, letters, digits, dot and underscore only. Stored lowercase.",
        example = "johndoe2",
    )
    @field:Size(min = 3, max = 50)
    @field:Pattern(
        regexp = "^[a-zA-Z0-9._]+$",
        message = "Username can contain only letters, digits, dot and underscore",
    )
    val username: String? = null,

    @field:Schema(description = "New first name", example = "John")
    @field:Size(max = 50)
    @field:Pattern(regexp = ".*\\S.*", message = "First name must not be blank")
    val firstName: String? = null,

    @field:Schema(description = "New last name", example = "Doe")
    @field:Size(max = 50)
    @field:Pattern(regexp = ".*\\S.*", message = "Last name must not be blank")
    val lastName: String? = null,

    @field:Schema(description = "New notifications preference", example = "false")
    val notificationsEnabled: Boolean? = null,
)

@Schema(description = "Payload to confirm the deletion of the current user's account")
data class DeleteAccountRequestDTO(
    @field:Schema(description = "Current password, required to confirm the deletion", format = "password")
    @field:NotBlank
    val password: String,
)