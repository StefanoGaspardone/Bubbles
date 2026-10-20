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

class AuthenticationRequiredException: RuntimeException("Authentication required")

class AccessTokenExpiredException: RuntimeException("Access token expired")

class InvalidAccessTokenException: RuntimeException("Invalid access token")

class UserNotFoundException(val userId: UUID): RuntimeException("User not found")

class IncorrectPasswordException: RuntimeException("Incorrect password")

class GearNotFoundException(val gearId: UUID): RuntimeException("Gear not found")

class InvalidGearAttributesException(message: String): RuntimeException(message)

class DiveNotFoundException(val diveId: UUID): RuntimeException("Dive not found")

class DiveSiteNotFoundException(val siteId: UUID): RuntimeException("Dive site not found")

class InvalidDiveException(message: String): RuntimeException(message)

class DiveOverlapException: RuntimeException("This dive overlaps another dive in your logbook")

class DiveSiteNotEditableException(val siteId: UUID): RuntimeException("Public dive sites cannot be modified or deleted")

class DiveSiteInUseException(val diveCount: Long): RuntimeException("Dive site is used by $diveCount dives and cannot be deleted")

class InvalidDiveSiteException(message: String): RuntimeException(message)

class CertificationNotFoundException(val certificationId: UUID): RuntimeException("Certification not found")

class InvalidCertificationException(message: String): RuntimeException(message)

class InvalidPaginationException(message: String): RuntimeException(message)

class EmptyFileException: RuntimeException("File is empty")

class FileTooLargeException(val maxSizeBytes: Long): RuntimeException("File is too large: maximum size is ${maxSizeBytes / (1024 * 1024)} MB")

class InvalidFileTypeException(message: String): RuntimeException(message)

class UploadedFileNotFoundException(val fileId: UUID): RuntimeException("File not found")

class StorageWriteException(val objectKey: String): RuntimeException("Unable to store the file, please try again later")

class StorageReadException(val objectKey: String): RuntimeException("File temporarily unavailable, please try again later")

class StorageNotFoundException(val objectKey: String): RuntimeException("File not found")

class StorageDeleteException(val objectKey: String): RuntimeException("Unable to delete the file")

class TooManyFilesException(val max: Int): RuntimeException("Maximum number of files reached ($max)")