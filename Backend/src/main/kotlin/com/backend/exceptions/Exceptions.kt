package com.backend.exceptions

import java.util.UUID

private const val INVALID_SESSION_MESSAGE = "Invalid or expired session, please log in again"

class EmailAlreadyTakenException(email: String): RuntimeException("Email already taken: $email")

class UsernameAlreadyTakenException(username: String): RuntimeException("Username already taken: $username")

class InvalidCredentialsException: RuntimeException("Invalid credentials")

class AccountNotActivatedException: RuntimeException("Account is not activated")

class AccountAlreadyActivatedException: RuntimeException("Account is not pending activation")

class AccountDisabledException: RuntimeException("Account is disabled")

class UserNotFoundByIdentifierException(identifier: String): RuntimeException("User not found for identifier: $identifier")

class EmailVerificationNotFoundException(val userId: UUID): RuntimeException("No pending email verification for this account")

class PasswordResetNotFoundException(val userId: UUID): RuntimeException("No pending password reset for this account")

class InvalidOtpException: RuntimeException("Invalid OTP code")

class OtpExpiredException: RuntimeException("OTP code expired")

class OtpMaxAttemptsExceededException: RuntimeException("Maximum OTP attempts exceeded")

class OtpResendCooldownException(val secondsRemaining: Long): RuntimeException("OTP resend cooldown active, try again in $secondsRemaining seconds")

class RefreshTokenNotFoundException: RuntimeException(INVALID_SESSION_MESSAGE)

class RefreshTokenExpiredOrRevokedException: RuntimeException(INVALID_SESSION_MESSAGE)

class RefreshTokenReuseDetectedException: RuntimeException(INVALID_SESSION_MESSAGE)