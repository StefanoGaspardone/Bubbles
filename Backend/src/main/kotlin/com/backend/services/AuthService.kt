package com.backend.services

import com.backend.exceptions.*
import com.backend.models.dtos.*
import com.backend.models.entities.EmailVerification
import com.backend.models.entities.PasswordReset
import com.backend.models.entities.RefreshToken
import com.backend.models.entities.User
import com.backend.models.enums.AccountStatus
import com.backend.properties.JwtProperties
import com.backend.properties.OtpProperties
import com.backend.repositories.EmailVerificationRepository
import com.backend.repositories.PasswordResetRepository
import com.backend.repositories.RefreshTokenRepository
import com.backend.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val passwordResetRepository: PasswordResetRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailService: EmailService,
    private val jwtProperties: JwtProperties,
    private val otpProperties: OtpProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureRandom = SecureRandom()

    companion object {
        private const val DUMMY_PASSWORD_HASH = $$"$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5uac.Hs2HL8YXtxjKmlXXNzDmSw6C"
    }

    // =========================================================================
    // Signup and activation
    // =========================================================================

    @Transactional
    fun signup(request: RegisterRequestDTO): RegisterResponseDTO {
        logger.debug("\n\t[DEBUG] [auth_service][signup] Signup attempt for email {}", request.email)

        try {
            val normalizedEmail = normalize(request.email)
            val normalizedUsername = normalize(request.username)

            if(userRepository.existsByEmail(normalizedEmail)) {
                throw EmailAlreadyTakenException(normalizedEmail)
            }

            if(userRepository.existsByUsername(normalizedUsername)) {
                throw UsernameAlreadyTakenException(normalizedUsername)
            }

            val now = Instant.now()
            val user = userRepository.save(
                User(
                    firstName = request.firstName.trim(),
                    lastName = request.lastName.trim(),
                    username = normalizedUsername,
                    email = normalizedEmail,
                    passwordHash = passwordEncoder.encode(request.password)!!,
                    accountStatus = AccountStatus.PENDING_ACTIVATION,
                )
            )

            val rawOtp = generateOtp()
            emailVerificationRepository.save(
                EmailVerification(
                    user = user,
                    otpHash = passwordEncoder.encode(rawOtp)!!,
                    expiresAt = now.plus(otpProperties.ttlMinutes, ChronoUnit.MINUTES),
                    attempts = 0,
                    lastSentAt = now,
                )
            )
            emailService.sendActivationOtp(user.email, rawOtp, otpProperties.ttlMinutes)

            logger.info("\n\t[INFO] [auth_service][signup] User {} signed up, OTP issued", user.id)
            return RegisterResponseDTO(email = user.email, message = "Please check your email for a verification code")
        } catch(e: EmailAlreadyTakenException) {
            logger.warn("\n\t[WARN] [auth_service][signup] Email already taken: {}", request.email)
            throw e
        } catch(e: UsernameAlreadyTakenException) {
            logger.warn("\n\t[WARN] [auth_service][signup] Username already taken: {}", request.username)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [auth_service][signup] Error signing up with email {}: {}", request.email, e.message)
            throw e
        }
    }

    @Transactional(noRollbackFor = [InvalidOtpException::class])
    fun activateAccount(request: ActivateAccountRequestDTO) {
        logger.debug("\n\t[DEBUG] [auth_service][activate_account] Activating account for identifier {}", request.identifier)

        try {
            val user = findByIdentifier(request.identifier)
            user.ensurePendingActivation()

            val verification = emailVerificationRepository.findByUserId(user.id!!)
                .orElseThrow { EmailVerificationNotFoundException(user.id!!) }

            if(verification.isExpired()) {
                throw OtpExpiredException()
            }

            if(verification.hasExceededMaxAttempts(otpProperties.maxAttempts)) {
                throw OtpMaxAttemptsExceededException()
            }

            if(!passwordEncoder.matches(request.otpCode.trim(), verification.otpHash)) {
                verification.attempts += 1
                throw InvalidOtpException()
            }

            user.activate()
            emailVerificationRepository.delete(verification)

            logger.info("\n\t[INFO] [auth_service][activate_account] Account activated for user {}", user.id)
        } catch(e: UserNotFoundByIdentifierException) {
            logger.warn("\n\t[WARN] [auth_service][activate_account] User not found for identifier {}", request.identifier)
            throw e
        } catch(e: AccountAlreadyActivatedException) {
            logger.warn("\n\t[WARN] [auth_service][activate_account] Account not pending activation for identifier {}", request.identifier)
            throw e
        } catch(e: EmailVerificationNotFoundException) {
            logger.warn("\n\t[WARN] [auth_service][activate_account] No pending verification for user {}", e.userId)
            throw e
        } catch(e: InvalidOtpException) {
            logger.warn("\n\t[WARN] [auth_service][activate_account] Invalid OTP for identifier {}", request.identifier)
            throw e
        } catch(e: OtpExpiredException) {
            logger.warn("\n\t[WARN] [auth_service][activate_account] Expired OTP for identifier {}", request.identifier)
            throw e
        } catch(e: OtpMaxAttemptsExceededException) {
            logger.warn("\n\t[WARN] [auth_service][activate_account] Max attempts exceeded for identifier {}", request.identifier)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [auth_service][activate_account] Error activating account for identifier {}: {}", request.identifier, e.message)
            throw e
        }
    }

    @Transactional
    fun resendActivationOtp(request: ResendOtpRequestDTO) {
        logger.debug("\n\t[DEBUG] [auth_service][resend_activation_otp] Resending activation OTP for identifier {}", request.identifier)

        try {
            val now = Instant.now()
            val user = findByIdentifier(request.identifier)
            val verification = emailVerificationRepository.findByUserId(user.id!!)
                .orElseThrow { EmailVerificationNotFoundException(user.id!!) }

            val remaining = remainingCooldownSeconds(verification.lastSentAt, now)
            if(remaining > 0) {
                throw OtpResendCooldownException(remaining)
            }

            val rawOtp = generateOtp()
            verification.otpHash = passwordEncoder.encode(rawOtp)!!
            verification.expiresAt = now.plus(otpProperties.ttlMinutes, ChronoUnit.MINUTES)
            verification.attempts = 0
            verification.lastSentAt = now

            emailService.sendActivationOtp(user.email, rawOtp, otpProperties.ttlMinutes)

            logger.info("\n\t[INFO] [auth_service][resend_activation_otp] Activation OTP resent for user {}", user.id)
        } catch(e: UserNotFoundByIdentifierException) {
            logger.warn("\n\t[WARN] [auth_service][resend_activation_otp] User not found for identifier {}", request.identifier)
            throw e
        } catch(e: EmailVerificationNotFoundException) {
            logger.warn("\n\t[WARN] [auth_service][resend_activation_otp] No pending verification for user {}", e.userId)
            throw e
        } catch(e: OtpResendCooldownException) {
            logger.warn("\n\t[WARN] [auth_service][resend_activation_otp] Resend cooldown active for identifier {}", request.identifier)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [auth_service][resend_activation_otp] Error resending OTP for identifier {}: {}", request.identifier, e.message)
            throw e
        }
    }

    // =========================================================================
    // Session
    // =========================================================================

    @Transactional
    fun login(request: LoginRequestDTO): AuthResponseDTO {
        logger.debug("\n\t[DEBUG] [auth_service][login] Login attempt for identifier {}", request.identifier)

        try {
            val now = Instant.now()
            val user = findByIdentifierOrNull(request.identifier)

            if(user == null) {
                passwordEncoder.matches(request.password, DUMMY_PASSWORD_HASH)
                throw InvalidCredentialsException()
            }

            if(!passwordEncoder.matches(request.password, user.passwordHash)) {
                throw InvalidCredentialsException()
            }

            ensureCanAuthenticate(user)

            user.recordLogin()
            val response = issueTokenPair(user, familyId = UUID.randomUUID(), now = now)

            logger.info("\n\t[INFO] [auth_service][login] User {} logged in successfully", user.id)
            return response
        } catch(e: InvalidCredentialsException) {
            logger.warn("\n\t[WARN] [auth_service][login] Invalid credentials for identifier {}", request.identifier)
            throw e
        } catch(e: AccountNotActivatedException) {
            logger.warn("\n\t[WARN] [auth_service][login] Account not activated for identifier {}", request.identifier)
            throw e
        } catch(e: AccountDisabledException) {
            logger.warn("\n\t[WARN] [auth_service][login] Account disabled for identifier {}", request.identifier)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [auth_service][login] Error logging in with identifier {}: {}", request.identifier, e.message)
            throw e
        }
    }

    @Transactional(noRollbackFor = [RefreshTokenReuseDetectedException::class])
    fun refresh(rawRefreshToken: String): RefreshResponseDTO {
        logger.debug("\n\t[DEBUG] [auth_service][refresh] Refresh attempt")

        try {
            val now = Instant.now()
            val storedToken = refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .orElseThrow { RefreshTokenNotFoundException() }

            if(storedToken.isRevoked()) {
                val revoked = refreshTokenRepository.revokeFamily(storedToken.familyId, now)

                logger.warn("\n\t[WARN] [auth_service][refresh] Reuse detected for family {}, {} tokens revoked", storedToken.familyId, revoked)
                throw RefreshTokenReuseDetectedException()
            }

            if(storedToken.isExpired()) {
                throw RefreshTokenExpiredOrRevokedException()
            }

            val user = storedToken.user
            ensureCanAuthenticate(user)

            storedToken.revoke()
            val response = issueTokenPair(user, familyId = storedToken.familyId, now = now)

            logger.info("\n\t[INFO] [auth_service][refresh] Access token refreshed for user {}", user.id)
            return RefreshResponseDTO(accessToken = response.accessToken, refreshToken = response.refreshToken)
        } catch(e: RefreshTokenNotFoundException) {
            logger.warn("\n\t[WARN] [auth_service][refresh] Refresh token not found")
            throw e
        } catch(e: RefreshTokenReuseDetectedException) {
            throw e
        } catch(e: RefreshTokenExpiredOrRevokedException) {
            logger.warn("\n\t[WARN] [auth_service][refresh] Refresh token expired")
            throw e
        } catch(e: AccountNotActivatedException) {
            logger.warn("\n\t[WARN] [auth_service][refresh] Account not activated")
            throw e
        } catch(e: AccountDisabledException) {
            logger.warn("\n\t[WARN] [auth_service][refresh] Account disabled")
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [auth_service][refresh] Error refreshing session: {}", e.message)
            throw e
        }
    }

    @Transactional
    fun logout(rawRefreshToken: String) {
        logger.debug("\n\t[DEBUG] [auth_service][logout] Logout attempt")

        try {
            refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .ifPresent { it.revoke() }

            logger.info("\n\t[INFO] [auth_service][logout] Refresh token revoked")
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [auth_service][logout] Error logging out: {}", e.message)
            throw e
        }
    }

    // =========================================================================
    // Password reset
    // =========================================================================

    @Transactional
    fun forgotPassword(request: ForgotPasswordRequestDTO) {
        logger.debug("\n\t[DEBUG] [auth_service][forgot_password] Password reset requested for identifier {}", request.identifier)

        try {
            val user = findByIdentifierOrNull(request.identifier)

            if(user == null) {
                logger.info("\n\t[INFO] [auth_service][forgot_password] No account found for identifier {}, responding generically", request.identifier)
                return
            }

            val now = Instant.now()
            val existing = passwordResetRepository.findByUserId(user.id!!).orElse(null)

            if(existing != null && remainingCooldownSeconds(existing.lastSentAt, now) > 0) {
                logger.info("\n\t[INFO] [auth_service][forgot_password] Cooldown active for user {}, responding generically", user.id)
                return
            }

            val rawOtp = generateOtp()
            val otpHash = passwordEncoder.encode(rawOtp)!!
            val expiresAt = now.plus(otpProperties.ttlMinutes, ChronoUnit.MINUTES)

            if(existing != null) {
                existing.otpHash = otpHash
                existing.expiresAt = expiresAt
                existing.attempts = 0
                existing.lastSentAt = now
            } else {
                passwordResetRepository.save(
                    PasswordReset(
                        user = user,
                        otpHash = otpHash,
                        expiresAt = expiresAt,
                        attempts = 0,
                        lastSentAt = now,
                    )
                )
            }

            emailService.sendPasswordResetOtp(user.email, rawOtp, otpProperties.ttlMinutes)

            logger.info("\n\t[INFO] [auth_service][forgot_password] Password reset OTP sent for user {}", user.id)
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [auth_service][forgot_password] Error requesting password reset for identifier {}: {}", request.identifier, e.message)
            throw e
        }
    }

    @Transactional(noRollbackFor = [InvalidOtpException::class])
    fun resetPassword(request: ResetPasswordRequestDTO) {
        logger.debug("\n\t[DEBUG] [auth_service][reset_password] Completing password reset for identifier {}", request.identifier)

        try {
            val now = Instant.now()
            val user = findByIdentifier(request.identifier)
            val reset = passwordResetRepository.findByUserId(user.id!!)
                .orElseThrow { PasswordResetNotFoundException(user.id!!) }

            if(reset.isExpired()) {
                throw OtpExpiredException()
            }

            if(reset.hasExceededMaxAttempts(otpProperties.maxAttempts)) {
                throw OtpMaxAttemptsExceededException()
            }

            if(!passwordEncoder.matches(request.otpCode.trim(), reset.otpHash)) {
                reset.attempts += 1
                throw InvalidOtpException()
            }

            user.passwordHash = passwordEncoder.encode(request.newPassword)!!
            passwordResetRepository.delete(reset)

            val revoked = refreshTokenRepository.revokeAllByUserId(user.id!!, now)

            logger.info("\n\t[INFO] [auth_service][reset_password] Password reset for user {}, {} sessions revoked", user.id, revoked)
        } catch(e: UserNotFoundByIdentifierException) {
            logger.warn("\n\t[WARN] [auth_service][reset_password] User not found for identifier {}", request.identifier)
            throw e
        } catch(e: PasswordResetNotFoundException) {
            logger.warn("\n\t[WARN] [auth_service][reset_password] No pending reset for user {}", e.userId)
            throw e
        } catch(e: InvalidOtpException) {
            logger.warn("\n\t[WARN] [auth_service][reset_password] Invalid OTP for identifier {}", request.identifier)
            throw e
        } catch(e: OtpExpiredException) {
            logger.warn("\n\t[WARN] [auth_service][reset_password] Expired OTP for identifier {}", request.identifier)
            throw e
        } catch(e: OtpMaxAttemptsExceededException) {
            logger.warn("\n\t[WARN] [auth_service][reset_password] Max attempts exceeded for identifier {}", request.identifier)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [auth_service][reset_password] Error resetting password for identifier {}: {}", request.identifier, e.message)
            throw e
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun issueTokenPair(user: User, familyId: UUID, now: Instant): AuthResponseDTO {
        val accessToken = jwtService.generateAccessToken(user.id!!, user.role.name)
        val rawRefreshToken = generateOpaqueToken()

        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = sha256(rawRefreshToken),
                familyId = familyId,
                expiresAt = now.plus(jwtProperties.refreshTokenTtlDays, ChronoUnit.DAYS),
            )
        )

        return AuthResponseDTO(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            user = UserDTO.from(user),
        )
    }

    private fun ensureCanAuthenticate(user: User) {
        when(user.accountStatus) {
            AccountStatus.ACTIVE -> Unit
            AccountStatus.PENDING_ACTIVATION -> throw AccountNotActivatedException()
            else -> throw AccountDisabledException()
        }
    }

    private fun findByIdentifier(identifier: String): User =
        findByIdentifierOrNull(identifier) ?: throw UserNotFoundByIdentifierException(identifier)

    private fun findByIdentifierOrNull(identifier: String): User? {
        val normalized = normalize(identifier)
        val result = if(normalized.contains("@")) {
            userRepository.findByEmail(normalized)
        } else {
            userRepository.findByUsername(normalized)
        }

        return result.orElse(null)
    }

    private fun remainingCooldownSeconds(lastSentAt: Instant, now: Instant): Long {
        val elapsed = Duration.between(lastSentAt, now).seconds
        return (otpProperties.resendCooldownSeconds - elapsed).coerceAtLeast(0)
    }

    private fun normalize(value: String): String =
        value.trim().lowercase()

    private fun generateOtp(): String =
        (100_000 + secureRandom.nextInt(900_000)).toString()

    private fun generateOpaqueToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(raw: String): String =
        MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
}