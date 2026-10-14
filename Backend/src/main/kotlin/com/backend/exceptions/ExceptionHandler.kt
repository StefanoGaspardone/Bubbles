package com.backend.exceptions

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.time.Instant

@Schema(description = "Standard error response")
data class ErrorResponse(
    @field:Schema(description = "Timestamp when the error occurred")
    val timestamp: Instant = Instant.now(),

    @field:Schema(description = "HTTP status code")
    val status: Int,

    @field:Schema(description = "HTTP reason phrase")
    val error: String,

    @field:Schema(description = "Human-readable error message")
    val message: String,
)

@RestControllerAdvice
class ExceptionsHandler {

    private fun buildResponse(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
            ),
            status,
        )

    private fun buildResponse(status: HttpStatus, e: Exception): ResponseEntity<ErrorResponse> {
        val fallbackMessage = e::class.simpleName
            ?.removeSuffix("Exception")
            ?.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            ?.replaceFirstChar { it.uppercase() }
            ?: status.reasonPhrase

        val finalMessage = e.message.takeIf { !it.isNullOrBlank() } ?: fallbackMessage
        return buildResponse(status, finalMessage)
    }

    // =========================================================================
    // Validation and request body
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
            .ifBlank { "Invalid request data" }

        return buildResponse(HttpStatus.BAD_REQUEST, message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedBody(ex: HttpMessageNotReadableException) =
        buildResponse(HttpStatus.BAD_REQUEST, "Malformed or incomplete request body")

    // =========================================================================
    // 400 Bad Request
    // =========================================================================

    @ExceptionHandler(InvalidOtpException::class)
    fun handleInvalidOtp(ex: InvalidOtpException) =
        buildResponse(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler(OtpExpiredException::class)
    fun handleOtpExpired(ex: OtpExpiredException) =
        buildResponse(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler(IncorrectPasswordException::class)
    fun handleIncorrectPassword(ex: IncorrectPasswordException) =
        buildResponse(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler(InvalidGearAttributesException::class)
    fun handleInvalidGearAttributes(ex: InvalidGearAttributesException) =
        buildResponse(HttpStatus.BAD_REQUEST, ex)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException) =
        buildResponse(HttpStatus.BAD_REQUEST, "${ex.name}: invalid value '${ex.value}'")

    @ExceptionHandler(InvalidDiveException::class)
    fun handleInvalidDive(ex: InvalidDiveException) =
        buildResponse(HttpStatus.BAD_REQUEST, ex)

    // =========================================================================
    // 401 Unauthorized
    // =========================================================================

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException) =
        buildResponse(HttpStatus.UNAUTHORIZED, ex)

    @ExceptionHandler(RefreshTokenNotFoundException::class)
    fun handleRefreshTokenNotFound(ex: RefreshTokenNotFoundException) =
        buildResponse(HttpStatus.UNAUTHORIZED, ex)

    @ExceptionHandler(RefreshTokenExpiredOrRevokedException::class)
    fun handleRefreshTokenExpiredOrRevoked(ex: RefreshTokenExpiredOrRevokedException) =
        buildResponse(HttpStatus.UNAUTHORIZED, ex)

    @ExceptionHandler(RefreshTokenReuseDetectedException::class)
    fun handleRefreshTokenReuseDetected(ex: RefreshTokenReuseDetectedException) =
        buildResponse(HttpStatus.UNAUTHORIZED, ex)

    @ExceptionHandler(AuthenticationRequiredException::class)
    fun handleAuthenticationRequired(ex: AuthenticationRequiredException) =
        buildResponse(HttpStatus.UNAUTHORIZED, ex)

    @ExceptionHandler(AccessTokenExpiredException::class)
    fun handleAccessTokenExpired(ex: AccessTokenExpiredException) =
        buildResponse(HttpStatus.UNAUTHORIZED, ex)

    @ExceptionHandler(InvalidAccessTokenException::class)
    fun handleInvalidAccessToken(ex: InvalidAccessTokenException) =
        buildResponse(HttpStatus.UNAUTHORIZED, ex)

    // =========================================================================
    // 403 Forbidden
    // =========================================================================

    @ExceptionHandler(AccountNotActivatedException::class)
    fun handleAccountNotActivated(ex: AccountNotActivatedException) =
        buildResponse(HttpStatus.FORBIDDEN, ex)

    @ExceptionHandler(AccountDisabledException::class)
    fun handleAccountDisabled(ex: AccountDisabledException) =
        buildResponse(HttpStatus.FORBIDDEN, ex)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException) =
        buildResponse(HttpStatus.FORBIDDEN, "You do not have permission to access this resource")

    // =========================================================================
    // 404 Not Found
    // =========================================================================

    @ExceptionHandler(UserNotFoundByIdentifierException::class)
    fun handleUserNotFoundByIdentifier(ex: UserNotFoundByIdentifierException) =
        buildResponse(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler(EmailVerificationNotFoundException::class)
    fun handleEmailVerificationNotFound(ex: EmailVerificationNotFoundException) =
        buildResponse(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler(PasswordResetNotFoundException::class)
    fun handlePasswordResetNotFound(ex: PasswordResetNotFoundException) =
        buildResponse(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException) =
        buildResponse(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler(GearNotFoundException::class)
    fun handleGearNotFound(ex: GearNotFoundException) =
        buildResponse(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler(DiveNotFoundException::class)
    fun handleDiveNotFound(ex: DiveNotFoundException) =
        buildResponse(HttpStatus.NOT_FOUND, ex)

    @ExceptionHandler(DiveSiteNotFoundException::class)
    fun handleDiveSiteNotFound(ex: DiveSiteNotFoundException) =
        buildResponse(HttpStatus.NOT_FOUND, ex)

    // =========================================================================
    // 409 Conflict
    // =========================================================================

    @ExceptionHandler(EmailAlreadyTakenException::class)
    fun handleEmailAlreadyTaken(ex: EmailAlreadyTakenException) =
        buildResponse(HttpStatus.CONFLICT, ex)

    @ExceptionHandler(UsernameAlreadyTakenException::class)
    fun handleUsernameAlreadyTaken(ex: UsernameAlreadyTakenException) =
        buildResponse(HttpStatus.CONFLICT, ex)

    @ExceptionHandler(AccountAlreadyActivatedException::class)
    fun handleAccountAlreadyActivated(ex: AccountAlreadyActivatedException) =
        buildResponse(HttpStatus.CONFLICT, ex)

    @ExceptionHandler(DiveOverlapException::class)
    fun handleDiveOverlap(ex: DiveOverlapException) =
        buildResponse(HttpStatus.CONFLICT, ex)

    // =========================================================================
    // 429 Too Many Requests
    // =========================================================================

    @ExceptionHandler(OtpMaxAttemptsExceededException::class)
    fun handleOtpMaxAttemptsExceeded(ex: OtpMaxAttemptsExceededException) =
        buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex)

    @ExceptionHandler(OtpResendCooldownException::class)
    fun handleOtpResendCooldown(ex: OtpResendCooldownException): ResponseEntity<ErrorResponse> {
        val response = buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex)

        return ResponseEntity.status(response.statusCode)
            .header(HttpHeaders.RETRY_AFTER, ex.secondsRemaining.toString())
            .body(response.body)
    }
}