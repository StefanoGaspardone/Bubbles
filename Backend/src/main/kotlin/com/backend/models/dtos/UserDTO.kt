package com.backend.models.dtos

import com.backend.models.entities.User
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Representation of user")
data class UserDTO(
    @field:Schema(description = "Unique identifier of the user")
    val id: UUID,

    @field:Schema(description = "Username")
    val username: String,

    @field:Schema(description = "Email address")
    val email: String,

    @field:Schema(description = "First name")
    val firstName: String,

    @field:Schema(description = "Last name")
    val lastName: String,

    @field:Schema(description = "Role assigned to the user", example = "USER")
    val role: String,

    @field:Schema(description = "ID of the uploaded avatar file, if the user has one. Null if no custom avatar was uploaded.")
    val avatarId: UUID?,

    @field:Schema(description = "Whether the user has push/email notifications enabled")
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