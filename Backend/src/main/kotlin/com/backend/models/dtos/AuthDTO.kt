package com.backend.models.dtos

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "Payload for registering a new user account")
data class RegisterRequestDTO(
    @field:Schema(
        description = "Unique username: 3-50 characters, letters, digits, dot and underscore only. Stored lowercase.",
        example = "stivigas",
    )
    @field:NotBlank
    @field:Size(min = 3, max = 50)
    @field:Pattern(
        regexp = "^[a-zA-Z0-9._]+$",
        message = "Username can contain only letters, digits, dot and underscore",
    )
    val username: String,

    @field:Schema(description = "User email address. Stored lowercase.", example = "user@example.com")
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,

    @field:Schema(description = "User first name", example = "John")
    @field:NotBlank
    @field:Size(max = 50)
    val firstName: String,

    @field:Schema(description = "User last name", example = "Doe")
    @field:NotBlank
    @field:Size(max = 50)
    val lastName: String,

    @field:Schema(description = "Plain text password, will be hashed before storage", format = "password")
    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String,
)

@Schema(description = "Response returned after signup, before the account is activated")
data class RegisterResponseDTO(
    @field:Schema(description = "Email address the verification code was sent to", example = "user@example.com")
    val email: String,

    @field:Schema(description = "Human-readable instructions for the client", example = "Please check your email for a verification code")
    val message: String,
)

@Schema(description = "Payload to activate an account with an OTP code")
data class ActivateAccountRequestDTO(
    @field:Schema(description = "Email or username of the account", example = "user@example.com")
    @field:NotBlank
    val identifier: String,

    @field:Schema(description = "6-digit verification code sent via email", example = "482913")
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]{6}$", message = "OTP code must be 6 digits")
    val otpCode: String,
)

@Schema(description = "Payload to resend the activation OTP code")
data class ResendOtpRequestDTO(
    @field:Schema(description = "Email or username of the account", example = "user@example.com")
    @field:NotBlank
    val identifier: String,
)

@Schema(description = "Payload to log in with email or username")
data class LoginRequestDTO(
    @field:Schema(description = "Email or username", example = "user@example.com")
    @field:NotBlank
    val identifier: String,

    @field:Schema(description = "Plain text password", format = "password")
    @field:NotBlank
    val password: String,
)

@Schema(description = "Payload to refresh or revoke a session. Mobile clients only: web clients send the refresh token as HttpOnly cookie and no body.")
data class RefreshTokenRequestDTO(
    @field:Schema(description = "Opaque refresh token previously issued at login or refresh")
    @field:NotBlank
    val refreshToken: String,
)

@Schema(description = "Access token (and, for mobile clients, refresh token) returned after successful authentication")
data class AuthResponseDTO(
    @field:Schema(description = "Short-lived JWT used to authenticate API requests")
    val accessToken: String,

    @field:Schema(
        description = "Long-lived opaque token used to obtain a new access token. Returned in the body only to mobile clients (X-Client-Type: MOBILE); web clients receive it as HttpOnly cookie.",
        nullable = true,
    )
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val refreshToken: String?,

    @field:Schema(description = "Authenticated user's public profile")
    val user: UserDTO,
)

@Schema(description = "New tokens returned after a successful refresh")
data class RefreshResponseDTO(
    @field:Schema(description = "New short-lived JWT")
    val accessToken: String,

    @field:Schema(
        description = "New long-lived opaque token, replaces the one used in the request. Returned in the body only to mobile clients (X-Client-Type: MOBILE); web clients receive it as HttpOnly cookie.",
        nullable = true,
    )
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val refreshToken: String?,
)

@Schema(description = "Payload to start a password reset")
data class ForgotPasswordRequestDTO(
    @field:Schema(description = "Email or username of the account", example = "user@example.com")
    @field:NotBlank
    val identifier: String,
)

@Schema(description = "Payload to complete a password reset")
data class ResetPasswordRequestDTO(
    @field:Schema(description = "Email or username of the account", example = "user@example.com")
    @field:NotBlank
    val identifier: String,

    @field:Schema(description = "6-digit verification code sent via email", example = "482913")
    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]{6}$", message = "OTP code must be 6 digits")
    val otpCode: String,

    @field:Schema(description = "New plain text password", format = "password")
    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val newPassword: String,
)