package com.backend.controllers

import com.backend.exceptions.ErrorResponse
import com.backend.exceptions.RefreshTokenNotFoundException
import com.backend.models.dtos.ActivateAccountRequestDTO
import com.backend.models.dtos.AuthResponseDTO
import com.backend.models.dtos.ForgotPasswordRequestDTO
import com.backend.models.dtos.LoginRequestDTO
import com.backend.models.dtos.RefreshResponseDTO
import com.backend.models.dtos.RefreshTokenRequestDTO
import com.backend.models.dtos.RegisterRequestDTO
import com.backend.models.dtos.RegisterResponseDTO
import com.backend.models.dtos.ResendOtpRequestDTO
import com.backend.models.dtos.ResetPasswordRequestDTO
import com.backend.security.CLIENT_TYPE_HEADER
import com.backend.security.ClientType
import com.backend.security.RefreshTokenCookies
import com.backend.services.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val CLIENT_TYPE_DESCRIPTION = "Client type. WEB (default): the refresh token travels only in an HttpOnly cookie. MOBILE: the refresh token travels in the request and response body."

@Tag(name = "Auth", description = "REST APIs for authentication: signup, activation, login, token refresh and password reset")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val refreshTokenCookies: RefreshTokenCookies,
) {

    @Operation(
        summary = "Signup",
        description = "Create a new account and send a 6-digit verification code via email. The account must be activated with /activate before logging in."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201", description = "Created - Account created, verification code sent",
                content = [Content(schema = Schema(implementation = RegisterResponseDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid signup data",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"email: must be a well-formed email address; password: size must be between 8 and 72\"}"
                    ), ExampleObject(
                        name = "MalformedBodyExample",
                        summary = "Malformed body example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Malformed or incomplete request body\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "409", description = "Conflict - Email or username already taken",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "EmailTakenExample",
                        summary = "Email already taken example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":409,\"error\":\"Conflict\",\"message\":\"Email already taken: mario.rossi@example.com\"}"
                    ), ExampleObject(
                        name = "UsernameTakenExample",
                        summary = "Username already taken example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":409,\"error\":\"Conflict\",\"message\":\"Username already taken: mario.rossi\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: RegisterRequestDTO): ResponseEntity<RegisterResponseDTO> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request))

    @Operation(
        summary = "Activate account",
        description = "Verify the OTP sent via email and activate the account. Does not log the user in: call /login afterwards."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Account successfully activated",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid data, or OTP invalid or expired",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"otpCode: OTP code must be 6 digits\"}"
                    ), ExampleObject(
                        name = "InvalidOtpExample",
                        summary = "Invalid OTP example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Invalid OTP code\"}"
                    ), ExampleObject(
                        name = "ExpiredOtpExample",
                        summary = "Expired OTP example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"OTP code expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - User not found or no pending verification",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "UserNotFoundExample",
                        summary = "User not found example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"User not found for identifier: mario.rossi@example.com\"}"
                    ), ExampleObject(
                        name = "VerificationNotFoundExample",
                        summary = "No pending verification example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"No pending email verification for this account\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "409", description = "Conflict - Account is not pending activation (already active, suspended or deleted)",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "NotPendingExample",
                        summary = "Account not pending activation example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":409,\"error\":\"Conflict\",\"message\":\"Account is not pending activation\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "429", description = "Too Many Requests - Maximum OTP attempts exceeded, a new code must be requested",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "MaxAttemptsExample",
                        summary = "Max attempts exceeded example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Maximum OTP attempts exceeded\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/activate")
    fun activate(@Valid @RequestBody request: ActivateAccountRequestDTO): ResponseEntity<Unit> {
        authService.activateAccount(request)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Resend activation OTP",
        description = "Send a new verification code, invalidating the previous one and resetting the attempts counter. Subject to a cooldown between requests."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - New verification code sent",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid request data",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"identifier: must not be blank\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - User not found or no pending verification (account already activated)",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "UserNotFoundExample",
                        summary = "User not found example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"User not found for identifier: mario.rossi@example.com\"}"
                    ), ExampleObject(
                        name = "VerificationNotFoundExample",
                        summary = "No pending verification example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"No pending email verification for this account\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "429", description = "Too Many Requests - Resend cooldown still active",
                headers = [Header(
                    name = "Retry-After",
                    description = "Seconds to wait before requesting a new code",
                    schema = Schema(type = "integer", example = "42")
                )],
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "CooldownExample",
                        summary = "Resend cooldown example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"OTP resend cooldown active, try again in 42 seconds\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/activate/resend")
    fun resendActivationOtp(@Valid @RequestBody request: ResendOtpRequestDTO): ResponseEntity<Unit> {
        authService.resendActivationOtp(request)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Login",
        description = "Authenticate with email or username and password. Returns a short-lived access token and the user profile. Web clients receive the refresh token as HttpOnly cookie (scoped to /api/v1/auth); mobile clients (X-Client-Type: MOBILE) receive it in the body."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Successfully authenticated",
                headers = [Header(
                    name = "Set-Cookie",
                    description = "Web clients only: HttpOnly cookie with the refresh token",
                    schema = Schema(type = "string")
                )],
                content = [Content(schema = Schema(implementation = AuthResponseDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid request data or client type",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"password: must not be blank\"}"
                    ), ExampleObject(
                        name = "InvalidClientTypeExample",
                        summary = "Invalid client type example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"X-Client-Type: invalid value 'DESKTOP'\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized - Invalid credentials",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidCredentialsExample",
                        summary = "Invalid credentials example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid credentials\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "403", description = "Forbidden - Account not activated, or suspended/deleted",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "AccountNotActivatedExample",
                        summary = "Account not activated example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Account is not activated\"}"
                    ), ExampleObject(
                        name = "AccountDisabledExample",
                        summary = "Account disabled example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Account is disabled\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequestDTO,
        @Parameter(description = CLIENT_TYPE_DESCRIPTION, example = "WEB")
        @RequestHeader(name = CLIENT_TYPE_HEADER, required = false) clientType: ClientType?,
    ): ResponseEntity<AuthResponseDTO> {
        val result = authService.login(request)

        return tokenResponse(
            clientType = clientType,
            rawRefreshToken = result.refreshToken!!,
            mobileBody = result,
            webBody = result.copy(refreshToken = null),
        )
    }

    @Operation(
        summary = "Refresh access token",
        description = "Exchange a valid refresh token for a new access token and a new refresh token. Web clients send no body: the refresh token is read from the HttpOnly cookie and the new one is set in the same cookie. Mobile clients (X-Client-Type: MOBILE) send and receive it in the body. The refresh token used is revoked and must not be reused: reusing it revokes the whole session and requires a new login."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Successfully refreshed",
                headers = [Header(
                    name = "Set-Cookie",
                    description = "Web clients only: HttpOnly cookie with the new refresh token",
                    schema = Schema(type = "string")
                )],
                content = [Content(schema = Schema(implementation = RefreshResponseDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid request data or client type",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example (mobile)",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"refreshToken: must not be blank\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized - Refresh token missing, unknown, expired, revoked or reused: the client must log in again",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidSessionExample",
                        summary = "Invalid session example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or expired session, please log in again\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "403", description = "Forbidden - Account not activated, or suspended/deleted",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "AccountNotActivatedExample",
                        summary = "Account not activated example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Account is not activated\"}"
                    ), ExampleObject(
                        name = "AccountDisabledExample",
                        summary = "Account disabled example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Account is disabled\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody(required = false) request: RefreshTokenRequestDTO?,
        @Parameter(description = CLIENT_TYPE_DESCRIPTION, example = "WEB")
        @RequestHeader(name = CLIENT_TYPE_HEADER, required = false) clientType: ClientType?,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<RefreshResponseDTO> {
        val rawRefreshToken = resolveRefreshToken(clientType, request, httpRequest)
            ?: throw RefreshTokenNotFoundException()

        val result = authService.refresh(rawRefreshToken)

        return tokenResponse(
            clientType = clientType,
            rawRefreshToken = result.refreshToken!!,
            mobileBody = result,
            webBody = result.copy(refreshToken = null),
        )
    }

    @Operation(
        summary = "Logout",
        description = "Revoke the current refresh token. Web clients send no body: the token is read from the cookie, and the cookie is always cleared. Mobile clients send the token in the body. Always returns 204, even if the token is missing, unknown or already revoked."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Successfully logged out",
                headers = [Header(
                    name = "Set-Cookie",
                    description = "Web clients only: expired cookie that removes the refresh token from the browser",
                    schema = Schema(type = "string")
                )],
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid request data or client type",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example (mobile)",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"refreshToken: must not be blank\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/logout")
    fun logout(
        @Valid @RequestBody(required = false) request: RefreshTokenRequestDTO?,
        @Parameter(description = CLIENT_TYPE_DESCRIPTION, example = "WEB")
        @RequestHeader(name = CLIENT_TYPE_HEADER, required = false) clientType: ClientType?,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Unit> {
        resolveRefreshToken(clientType, request, httpRequest)?.let { authService.logout(it) }

        return when(clientType ?: ClientType.WEB) {
            ClientType.MOBILE -> ResponseEntity.noContent().build()
            ClientType.WEB -> ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
                .build()
        }
    }

    @Operation(
        summary = "Forgot password",
        description = "Request a password reset code via email. Always returns 204, whether the account exists or not and whether the resend cooldown is active or not, to avoid revealing registered accounts."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - If an account exists and no cooldown is active, a reset code has been sent",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid request data",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"identifier: must not be blank\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/password/forgot")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequestDTO): ResponseEntity<Unit> {
        authService.forgotPassword(request)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Reset password",
        description = "Verify the OTP and set a new password. All active sessions of the user are revoked."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Password successfully reset",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid data, or OTP invalid or expired",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"newPassword: size must be between 8 and 72\"}"
                    ), ExampleObject(
                        name = "InvalidOtpExample",
                        summary = "Invalid OTP example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Invalid OTP code\"}"
                    ), ExampleObject(
                        name = "ExpiredOtpExample",
                        summary = "Expired OTP example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"OTP code expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - User not found or no pending password reset",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "UserNotFoundExample",
                        summary = "User not found example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"User not found for identifier: mario.rossi@example.com\"}"
                    ), ExampleObject(
                        name = "ResetNotFoundExample",
                        summary = "No pending reset example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"No pending password reset for this account\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "429", description = "Too Many Requests - Maximum OTP attempts exceeded, a new code must be requested",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "MaxAttemptsExample",
                        summary = "Max attempts exceeded example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Maximum OTP attempts exceeded\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/password/reset")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequestDTO): ResponseEntity<Unit> {
        authService.resetPassword(request)
        return ResponseEntity.noContent().build()
    }

    private fun resolveRefreshToken(
        clientType: ClientType?,
        body: RefreshTokenRequestDTO?,
        httpRequest: HttpServletRequest,
    ): String? =
        when(clientType ?: ClientType.WEB) {
            ClientType.WEB -> refreshTokenCookies.read(httpRequest)
            ClientType.MOBILE -> body?.refreshToken
        }

    private fun <T : Any> tokenResponse(
        clientType: ClientType?,
        rawRefreshToken: String,
        mobileBody: T,
        webBody: T,
    ): ResponseEntity<T> =
        when(clientType ?: ClientType.WEB) {
            ClientType.MOBILE -> ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mobileBody)

            ClientType.WEB -> ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.create(rawRefreshToken).toString())
                .body(webBody)
        }
}