package com.backend.services

import com.backend.exceptions.DiveSiteInUseException
import com.backend.exceptions.DiveSiteNotEditableException
import com.backend.exceptions.DiveSiteNotFoundException
import com.backend.exceptions.InvalidDiveSiteException
import com.backend.models.dtos.DiveSiteDTO
import com.backend.models.dtos.DiveSiteRequestDTO
import com.backend.models.dtos.PageDTO
import com.backend.models.dtos.toPageDTO
import com.backend.models.entities.DiveSite
import com.backend.models.enums.DiveSiteScope
import com.backend.models.enums.DiveSiteSource
import com.backend.models.enums.DiveSiteType
import com.backend.repositories.DiveRepository
import com.backend.repositories.DiveSiteRepository
import com.backend.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale
import java.util.UUID

@Service
class DiveSiteService(
    private val diveSiteRepository: DiveSiteRepository,
    private val diveRepository: DiveRepository,
    private val userRepository: UserRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val MAX_PAGE_SIZE = 100

        val ISO_COUNTRY_CODES: Set<String> = Locale.getISOCountries().toSet()
    }

    @Transactional(readOnly = true)
    fun listSites(userId: UUID, page: Int, size: Int, query: String?, siteType: DiveSiteType?, scope: DiveSiteScope): PageDTO<DiveSiteDTO> {
        logger.debug("\n\t[DEBUG] [dive_site_service][list_sites] Listing sites for user {} (page={}, size={}, query={}, siteType={}, scope={})", userId, page, size, query, siteType, scope)

        try {
            val errors = mutableListOf<String>()
            if(page < 0) errors += "page: must be greater than or equal to 0"
            if(size !in 1..MAX_PAGE_SIZE) errors += "size: must be between 1 and $MAX_PAGE_SIZE"

            if(errors.isNotEmpty()) {
                throw InvalidDiveSiteException(errors.joinToString("; "))
            }

            val (includeMine, includePublic) = when(scope) {
                DiveSiteScope.ALL -> true to true
                DiveSiteScope.MINE -> true to false
                DiveSiteScope.PUBLIC -> false to true
            }

            val result = diveSiteRepository.search(
                userId = userId,
                pattern = toLikePattern(query),
                siteType = siteType,
                includeMine = includeMine,
                includePublic = includePublic,
                pageable = PageRequest.of(page, size),
            )
            val diveCounts = diveCountsFor(userId, result.content)

            logger.debug("\n\t[DEBUG] [dive_site_service][list_sites] Found {} sites (total {}) for user {}", result.numberOfElements, result.totalElements, userId)
            return result.toPageDTO { DiveSiteDTO.from(it, diveCounts[it.id] ?: 0L) }
        } catch(e: InvalidDiveSiteException) {
            logger.warn("\n\t[WARN] [dive_site_service][list_sites] Invalid parameters for user {}: {}", userId, e.message)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_site_service][list_sites] Error listing sites for user {}: {}", userId, e.message)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun getSite(userId: UUID, siteId: UUID): DiveSiteDTO {
        logger.debug("\n\t[DEBUG] [dive_site_service][get_site] Retrieving site {} for user {}", siteId, userId)

        try {
            val site = findVisibleSite(userId, siteId)
            val diveCount = diveRepository.countBySiteIdAndOwnerId(siteId, userId)

            return DiveSiteDTO.from(site, diveCount)
        } catch(e: DiveSiteNotFoundException) {
            logger.warn("\n\t[WARN] [dive_site_service][get_site] Site {} not found for user {}", siteId, userId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_site_service][get_site] Error retrieving site {} for user {}: {}", siteId, userId, e.message)
            throw e
        }
    }

    @Transactional
    fun createSite(userId: UUID, request: DiveSiteRequestDTO): DiveSiteDTO {
        logger.debug("\n\t[DEBUG] [dive_site_service][create_site] Creating site for user {}", userId)

        try {
            validateRequest(request)

            val site = diveSiteRepository.save(
                DiveSite(
                    owner = userRepository.getReferenceById(userId),
                    name = request.name.trim(),
                    locality = request.locality.cleanOrNull(),
                    countryCode = request.countryCode.cleanOrNull()?.uppercase(),
                    latitude = request.latitude,
                    longitude = request.longitude,
                    siteType = request.siteType,
                    minDepthM = request.minDepthM,
                    maxDepthM = request.maxDepthM,
                    description = request.description.cleanOrNull(),
                    source = DiveSiteSource.USER,
                )
            )

            logger.info("\n\t[INFO] [dive_site_service][create_site] Site {} created for user {}", site.id, userId)
            return DiveSiteDTO.from(site, diveCount = 0L)
        } catch(e: InvalidDiveSiteException) {
            logger.warn("\n\t[WARN] [dive_site_service][create_site] Invalid site for user {}: {}", userId, e.message)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_site_service][create_site] Error creating site for user {}: {}", userId, e.message)
            throw e
        }
    }

    @Transactional
    fun updateSite(userId: UUID, siteId: UUID, request: DiveSiteRequestDTO): DiveSiteDTO {
        logger.debug("\n\t[DEBUG] [dive_site_service][update_site] Updating site {} for user {}", siteId, userId)

        try {
            val site = findEditableSite(userId, siteId)
            validateRequest(request)

            site.name = request.name.trim()
            site.locality = request.locality.cleanOrNull()
            site.countryCode = request.countryCode.cleanOrNull()?.uppercase()
            site.latitude = request.latitude
            site.longitude = request.longitude
            site.siteType = request.siteType
            site.minDepthM = request.minDepthM
            site.maxDepthM = request.maxDepthM
            site.description = request.description.cleanOrNull()

            val diveCount = diveRepository.countBySiteIdAndOwnerId(siteId, userId)

            logger.info("\n\t[INFO] [dive_site_service][update_site] Site {} updated for user {}", siteId, userId)
            return DiveSiteDTO.from(site, diveCount)
        } catch(e: DiveSiteNotFoundException) {
            logger.warn("\n\t[WARN] [dive_site_service][update_site] Site {} not found for user {}", siteId, userId)
            throw e
        } catch(e: DiveSiteNotEditableException) {
            logger.warn("\n\t[WARN] [dive_site_service][update_site] User {} tried to modify public site {}", userId, siteId)
            throw e
        } catch(e: InvalidDiveSiteException) {
            logger.warn("\n\t[WARN] [dive_site_service][update_site] Invalid site {} for user {}: {}", siteId, userId, e.message)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_site_service][update_site] Error updating site {} for user {}: {}", siteId, userId, e.message)
            throw e
        }
    }

    @Transactional
    fun deleteSite(userId: UUID, siteId: UUID) {
        logger.debug("\n\t[DEBUG] [dive_site_service][delete_site] Deleting site {} for user {}", siteId, userId)

        try {
            val site = findEditableSite(userId, siteId)

            val diveCount = diveRepository.countBySiteId(siteId)
            if(diveCount > 0) {
                throw DiveSiteInUseException(diveCount)
            }

            try {
                diveSiteRepository.delete(site)
                diveSiteRepository.flush()
            } catch(_: DataIntegrityViolationException) {
                throw DiveSiteInUseException(diveRepository.countBySiteId(siteId))
            }

            logger.info("\n\t[INFO] [dive_site_service][delete_site] Site {} deleted for user {}", siteId, userId)
        } catch(e: DiveSiteNotFoundException) {
            logger.warn("\n\t[WARN] [dive_site_service][delete_site] Site {} not found for user {}", siteId, userId)
            throw e
        } catch(e: DiveSiteNotEditableException) {
            logger.warn("\n\t[WARN] [dive_site_service][delete_site] User {} tried to delete public site {}", userId, siteId)
            throw e
        } catch(e: DiveSiteInUseException) {
            logger.warn("\n\t[WARN] [dive_site_service][delete_site] Site {} is used by {} dives", siteId, e.diveCount)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_site_service][delete_site] Error deleting site {} for user {}: {}", siteId, userId, e.message)
            throw e
        }
    }

    private fun validateRequest(request: DiveSiteRequestDTO) {
        val errors = mutableListOf<String>()

        if(request.minDepthM != null && request.maxDepthM != null && request.minDepthM > request.maxDepthM) {
            errors += "minDepthM: must not exceed maxDepthM"
        }

        request.countryCode.cleanOrNull()?.uppercase()?.let {
            if(it !in ISO_COUNTRY_CODES) errors += "countryCode: unknown ISO country code '$it'"
        }

        if(errors.isNotEmpty()) {
            throw InvalidDiveSiteException(errors.joinToString("; "))
        }
    }

    private fun findVisibleSite(userId: UUID, siteId: UUID): DiveSite =
        diveSiteRepository.findVisibleById(siteId, userId)
            .orElseThrow { DiveSiteNotFoundException(siteId) }

    private fun findEditableSite(userId: UUID, siteId: UUID): DiveSite {
        val site = findVisibleSite(userId, siteId)
        if(site.isPublic()) {
            throw DiveSiteNotEditableException(siteId)
        }

        return site
    }

    private fun diveCountsFor(userId: UUID, sites: List<DiveSite>): Map<UUID, Long> {
        if(sites.isEmpty()) return emptyMap()

        return diveRepository.countDivesBySite(userId, sites.map { it.id!! })
            .associate { it.siteId to it.diveCount }
    }

    private fun toLikePattern(query: String?): String {
        val text = query.cleanOrNull()?.lowercase() ?: return "%"

        val escaped = text
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

        return "%$escaped%"
    }

    private fun String?.cleanOrNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}