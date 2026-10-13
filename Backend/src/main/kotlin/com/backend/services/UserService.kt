package com.backend.services

import com.backend.exceptions.IncorrectPasswordException
import com.backend.exceptions.UserNotFoundException
import com.backend.exceptions.UsernameAlreadyTakenException
import com.backend.models.dtos.DeleteAccountRequestDTO
import com.backend.models.dtos.UpdateUserRequestDTO
import com.backend.models.dtos.UserDTO
import com.backend.models.entities.User
import com.backend.repositories.EmailVerificationRepository
import com.backend.repositories.PasswordResetRepository
import com.backend.repositories.RefreshTokenRepository
import com.backend.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val passwordResetRepository: PasswordResetRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getUserById(userId: UUID): UserDTO {
        logger.debug("\n\t[DEBUG] [user_service][get_user_by_id] Retrieving user {}", userId)

        try {
            val user = findUser(userId)

            logger.debug("\n\t[DEBUG] [user_service][get_user_by_id] Retrieved user {}", userId)
            return UserDTO.from(user)
        } catch(e: UserNotFoundException) {
            logger.warn("\n\t[WARN] [user_service][get_user_by_id] User {} not found", userId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [user_service][get_user_by_id] Error retrieving user {}: {}", userId, e.message)
            throw e
        }
    }

    @Transactional
    fun updateProfile(userId: UUID, request: UpdateUserRequestDTO): UserDTO {
        logger.debug("\n\t[DEBUG] [user_service][update_profile] Updating profile for user {}", userId)

        try {
            val user = findUser(userId)

            request.username?.let { newUsername ->
                val normalized = normalize(newUsername)

                if(normalized != user.username) {
                    if(userRepository.existsByUsername(normalized)) {
                        throw UsernameAlreadyTakenException(normalized)
                    }

                    user.username = normalized
                }
            }

            request.firstName?.let { user.firstName = it.trim() }
            request.lastName?.let { user.lastName = it.trim() }
            request.notificationsEnabled?.let { user.notificationsEnabled = it }

            logger.info("\n\t[INFO] [user_service][update_profile] Profile updated for user {}", userId)
            return UserDTO.from(user)
        } catch(e: UserNotFoundException) {
            logger.warn("\n\t[WARN] [user_service][update_profile] User {} not found", userId)
            throw e
        } catch(e: UsernameAlreadyTakenException) {
            logger.warn("\n\t[WARN] [user_service][update_profile] Username already taken for user {}: {}", userId, request.username)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [user_service][update_profile] Error updating profile for user {}: {}", userId, e.message)
            throw e
        }
    }

    @Transactional
    fun deleteAccount(userId: UUID, request: DeleteAccountRequestDTO) {
        logger.debug("\n\t[DEBUG] [user_service][delete_account] Deleting account for user {}", userId)

        try {
            val now = Instant.now()
            val user = findUser(userId)

            if(!passwordEncoder.matches(request.password, user.passwordHash)) {
                throw IncorrectPasswordException()
            }

            // TODO: when dives, gear and certifications exist, delete or anonymize them here

            user.markDeleted()

            val revoked = refreshTokenRepository.revokeAllByUserId(userId, now)
            emailVerificationRepository.deleteByUserId(userId)
            passwordResetRepository.deleteByUserId(userId)

            logger.info("\n\t[INFO] [user_service][delete_account] Account {} deleted, {} sessions revoked", userId, revoked)
        } catch(e: UserNotFoundException) {
            logger.warn("\n\t[WARN] [user_service][delete_account] User {} not found", userId)
            throw e
        } catch(e: IncorrectPasswordException) {
            logger.warn("\n\t[WARN] [user_service][delete_account] Incorrect password for user {}", userId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [user_service][delete_account] Error deleting account {}: {}", userId, e.message)
            throw e
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun findUser(userId: UUID): User =
        userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }

    private fun normalize(value: String): String =
        value.trim().lowercase()
}