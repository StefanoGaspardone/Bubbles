package com.backend.services

import com.backend.exceptions.IncorrectPasswordException
import com.backend.exceptions.InvalidPaginationException
import com.backend.exceptions.UserNotFoundException
import com.backend.exceptions.UsernameAlreadyTakenException
import com.backend.models.dtos.*
import com.backend.models.entities.User
import com.backend.models.enums.AccountStatus
import com.backend.models.specifications.UserSpecification
import com.backend.repositories.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class UserService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val passwordResetRepository: PasswordResetRepository,
    private val diveRepository: DiveRepository,
    private val diveSiteRepository: DiveSiteRepository,
    private val gearRepository: GearRepository,
    private val certificationRepository: CertificationRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val MAX_PAGE_SIZE = 100
    }

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
            var user = findUser(userId)

            if(!passwordEncoder.matches(request.password, user.passwordHash)) {
                throw IncorrectPasswordException()
            }

            val deletedDives = diveRepository.deleteByOwnerId(userId)
            val deletedSites = diveSiteRepository.deleteByOwnerId(userId)
            val deletedGear = gearRepository.deleteByOwnerId(userId)
            val deletedCertifications = certificationRepository.deleteByOwnerId(userId)

            user = findUser(userId)
            user.markDeleted()

            val revoked = refreshTokenRepository.revokeAllByUserId(userId, now)
            emailVerificationRepository.deleteByUserId(userId)
            passwordResetRepository.deleteByUserId(userId)

            logger.info("\n\t[INFO] [user_service][delete_account] Account {} deleted: {} dives, {} sites, {} gear, {} certifications removed, {} sessions revoked", userId, deletedDives, deletedSites, deletedGear, deletedCertifications, revoked)
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

    @Transactional(readOnly = true)
    fun listUsers(page: Int, size: Int, username: String?, accountStatus: AccountStatus?): PageDTO<UserLookupDTO> {
        logger.debug("\n\t[DEBUG] [user_service][list_users] Listing users (page={}, size={}, username={}, accountStatus={})", page, size, username, accountStatus)

        try {
            val errors = mutableListOf<String>()
            if(page < 0) errors += "page: must be greater than or equal to 0"
            if(size !in 1..MAX_PAGE_SIZE) errors += "size: must be between 1 and $MAX_PAGE_SIZE"

            if(errors.isNotEmpty()) {
                throw InvalidPaginationException(errors.joinToString("; "))
            }

            val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "username"))
            val result = userRepository.findAll(UserSpecification.withFilters(username, accountStatus), pageable)

            logger.debug("\n\t[DEBUG] [user_service][list_users] Found {} users (total {})", result.numberOfElements, result.totalElements)
            return result.toPageDTO(UserLookupDTO::from)
        } catch(e: InvalidPaginationException) {
            logger.warn("\n\t[WARN] [user_service][list_users] Invalid parameters: {}", e.message)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [user_service][list_users] Error listing users: {}", e.message)
            throw e
        }
    }

    private fun findUser(userId: UUID): User =
        userRepository.findById(userId)
            .orElseThrow { UserNotFoundException(userId) }

    private fun normalize(value: String): String =
        value.trim().lowercase()
}