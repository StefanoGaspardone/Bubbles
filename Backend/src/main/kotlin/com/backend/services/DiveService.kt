package com.backend.services

import com.backend.exceptions.DiveNotFoundException
import com.backend.exceptions.DiveOverlapException
import com.backend.exceptions.DiveSiteNotFoundException
import com.backend.exceptions.InvalidDiveException
import com.backend.exceptions.InvalidPaginationException
import com.backend.exceptions.TooManyFilesException
import com.backend.models.dtos.DiveBuddySelectionDTO
import com.backend.models.dtos.DiveConfirmationDTO
import com.backend.models.dtos.DiveDTO
import com.backend.models.dtos.DiveGearSelectionDTO
import com.backend.models.dtos.DiveRequestDTO
import com.backend.models.dtos.DiveSummaryDTO
import com.backend.models.dtos.PageDTO
import com.backend.models.dtos.UploadedFileDTO
import com.backend.models.dtos.toPageDTO
import com.backend.models.entities.Dive
import com.backend.models.entities.DiveBuddy
import com.backend.models.entities.DiveGear
import com.backend.models.entities.DiveSegment
import com.backend.models.entities.DiveSite
import com.backend.models.entities.DiveTank
import com.backend.models.enums.AccountStatus
import com.backend.models.enums.DiveSegmentType
import com.backend.models.enums.FileOwnerType
import com.backend.models.events.BuddyNotification
import com.backend.models.events.BuddyNotificationReason
import com.backend.models.events.BuddyNotificationsEvent
import com.backend.repositories.DiveBuddyRepository
import com.backend.repositories.DiveRepository
import com.backend.repositories.DiveSiteRepository
import com.backend.repositories.GearRepository
import com.backend.repositories.UserRepository
import com.backend.utils.DivePhysics
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.DateTimeException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.math.abs

@Service
class DiveService(
    private val diveRepository: DiveRepository,
    private val diveSiteRepository: DiveSiteRepository,
    private val diveBuddyRepository: DiveBuddyRepository,
    private val gearRepository: GearRepository,
    private val userRepository: UserRepository,
    private val uploadedFileService: UploadedFileService,
    private val eventPublisher: ApplicationEventPublisher,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        const val MAX_PAGE_SIZE = 100

        val MAX_DIVE_DURATION: Duration = Duration.ofHours(12)
        val FUTURE_TOLERANCE: Duration = Duration.ofMinutes(5)

        const val SEGMENTS_DURATION_TOLERANCE_SECONDS = 120L
        const val DEPTH_TOLERANCE_M = 0.5
        const val OVERLAP_CONSTRAINT = "ex_dives_no_overlap"
        const val MAX_PHOTOS_PER_DIVE = 20

        val TRANSITION_SEGMENTS = setOf(DiveSegmentType.DESCENT, DiveSegmentType.ASCENT, DiveSegmentType.SURFACE)
    }

    private data class DiveTimes(
        val startedAt: Instant,
        val endedAt: Instant,
        val zoneId: ZoneId,
    )

    private data class CertifiedData(
        val siteId: UUID?,
        val startedAt: Instant,
        val endedAt: Instant,
        val maxDepthM: Double,
    )

    private fun Dive.certifiedData() = CertifiedData(
        siteId = site.id,
        startedAt = startedAt,
        endedAt = endedAt,
        maxDepthM = maxDepthM,
    )

    @Transactional(readOnly = true)
    fun listDives(ownerId: UUID, page: Int, size: Int, siteId: UUID?, from: Instant?, to: Instant?): PageDTO<DiveSummaryDTO> {
        logger.debug("\n\t[DEBUG] [dive_service][list_dives] Listing dives for user {} (page={}, size={}, siteId={}, from={}, to={})", ownerId, page, size, siteId, from, to)

        try {
            val paginationErrors = mutableListOf<String>()
            if(page < 0) paginationErrors += "page: must be greater than or equal to 0"
            if(size !in 1..MAX_PAGE_SIZE) paginationErrors += "size: must be between 1 and $MAX_PAGE_SIZE"
            if(paginationErrors.isNotEmpty()) {
                throw InvalidPaginationException(paginationErrors.joinToString("; "))
            }

            if(from != null && to != null && !from.isBefore(to)) {
                throw InvalidDiveException("from: must be before to")
            }

            val result = diveRepository.search(ownerId, siteId, from, to, PageRequest.of(page, size))
            val numbers = diveNumbersFor(ownerId, result.content)

            logger.debug("\n\t[DEBUG] [dive_service][list_dives] Found {} dives (total {}) for user {}", result.numberOfElements, result.totalElements, ownerId)
            return result.toPageDTO { DiveSummaryDTO.from(it, numbers.getValue(it.id!!)) }
        } catch(e: InvalidPaginationException) {
            logger.warn("\n\t[WARN] [dive_service][list_dives] Invalid paging for user {}: {}", ownerId, e.message)
            throw e
        } catch(e: InvalidDiveException) {
            logger.warn("\n\t[WARN] [dive_service][list_dives] Invalid filters for user {}: {}", ownerId, e.message)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_service][list_dives] Error listing dives for user {}: {}", ownerId, e.message)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun getDive(ownerId: UUID, diveId: UUID): DiveDTO {
        logger.debug("\n\t[DEBUG] [dive_service][get_dive] Retrieving dive {} for user {}", diveId, ownerId)

        try {
            val dive = findOwnedDive(ownerId, diveId)
            return toDTO(ownerId, dive)
        } catch(e: DiveNotFoundException) {
            logger.warn("\n\t[WARN] [dive_service][get_dive] Dive {} not found for user {}", diveId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_service][get_dive] Error retrieving dive {} for user {}: {}", diveId, ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun createDive(ownerId: UUID, request: DiveRequestDTO): DiveDTO {
        logger.debug("\n\t[DEBUG] [dive_service][create_dive] Creating dive for user {}", ownerId)

        try {
            val times = validateRequest(request)
            ensureNoOverlap(ownerId, times, excludeDiveId = null)
            val site = findVisibleSite(ownerId, request.siteId)

            val dive = Dive(
                owner = userRepository.getReferenceById(ownerId),
                site = site,
                startedAt = times.startedAt,
                endedAt = times.endedAt,
                timeZone = times.zoneId.id,
                maxDepthM = request.maxDepthM,
            )
            val addedBuddies = applyRequest(ownerId, dive, request)

            val saved = saveAndFlush(dive)
            publishBuddyNotifications(saved, addedBuddies, BuddyNotificationReason.ADDED)

            logger.info("\n\t[INFO] [dive_service][create_dive] Dive {} created for user {} ({} buddies to notify)", saved.id, ownerId, addedBuddies.size)
            return toDTO(ownerId, saved)
        } catch(e: InvalidDiveException) {
            logger.warn("\n\t[WARN] [dive_service][create_dive] Invalid dive for user {}: {}", ownerId, e.message)
            throw e
        } catch(e: DiveOverlapException) {
            logger.warn("\n\t[WARN] [dive_service][create_dive] Overlapping dive for user {}", ownerId)
            throw e
        } catch(e: DiveSiteNotFoundException) {
            logger.warn("\n\t[WARN] [dive_service][create_dive] Dive site {} not found for user {}", request.siteId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_service][create_dive] Error creating dive for user {}: {}", ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun updateDive(ownerId: UUID, diveId: UUID, request: DiveRequestDTO): DiveDTO {
        logger.debug("\n\t[DEBUG] [dive_service][update_dive] Updating dive {} for user {}", diveId, ownerId)

        try {
            val dive = findOwnedDive(ownerId, diveId)
            val times = validateRequest(request)
            ensureNoOverlap(ownerId, times, excludeDiveId = diveId)

            val certifiedBefore = dive.certifiedData()

            dive.site = if(dive.site.id == request.siteId) dive.site else findVisibleSite(ownerId, request.siteId)
            dive.startedAt = times.startedAt
            dive.endedAt = times.endedAt
            dive.timeZone = times.zoneId.id
            dive.maxDepthM = request.maxDepthM
            val addedBuddies = applyRequest(ownerId, dive, request)

            val invalidatedBuddies = if(dive.certifiedData() != certifiedBefore) {
                dive.buddies.filter { it.resetConfirmation() }
            } else {
                emptyList()
            }

            val saved = saveAndFlush(dive)
            publishBuddyNotifications(saved, addedBuddies, BuddyNotificationReason.ADDED)
            publishBuddyNotifications(saved, invalidatedBuddies, BuddyNotificationReason.DATA_CHANGED)

            logger.info(
                "\n\t[INFO] [dive_service][update_dive] Dive {} updated for user {} ({} buddies added, {} confirmations invalidated)",
                diveId, ownerId, addedBuddies.size, invalidatedBuddies.size
            )
            return toDTO(ownerId, saved)
        } catch(e: DiveNotFoundException) {
            logger.warn("\n\t[WARN] [dive_service][update_dive] Dive {} not found for user {}", diveId, ownerId)
            throw e
        } catch(e: InvalidDiveException) {
            logger.warn("\n\t[WARN] [dive_service][update_dive] Invalid dive {} for user {}: {}", diveId, ownerId, e.message)
            throw e
        } catch(e: DiveOverlapException) {
            logger.warn("\n\t[WARN] [dive_service][update_dive] Dive {} would overlap another dive of user {}", diveId, ownerId)
            throw e
        } catch(e: DiveSiteNotFoundException) {
            logger.warn("\n\t[WARN] [dive_service][update_dive] Dive site {} not found for user {}", request.siteId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_service][update_dive] Error updating dive {} for user {}: {}", diveId, ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun deleteDive(ownerId: UUID, diveId: UUID) {
        logger.debug("\n\t[DEBUG] [dive_service][delete_dive] Deleting dive {} for user {}", diveId, ownerId)

        try {
            val dive = findOwnedDive(ownerId, diveId)

            uploadedFileService.deleteAllForOwner(FileOwnerType.DIVE_PHOTO, diveId)
            diveRepository.delete(dive)

            logger.info("\n\t[INFO] [dive_service][delete_dive] Dive {} deleted for user {}", diveId, ownerId)
        } catch(e: DiveNotFoundException) {
            logger.warn("\n\t[WARN] [dive_service][delete_dive] Dive {} not found for user {}", diveId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_service][delete_dive] Error deleting dive {} for user {}: {}", diveId, ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun confirmDive(buddyUserId: UUID, diveId: UUID): DiveConfirmationDTO {
        logger.debug("\n\t[DEBUG] [dive_service][confirm_dive] User {} confirming dive {}", buddyUserId, diveId)

        try {
            val buddy = diveBuddyRepository.findForConfirmation(diveId, buddyUserId)
                .orElseThrow { DiveNotFoundException(diveId) }

            buddy.confirm(Instant.now())

            logger.info("\n\t[INFO] [dive_service][confirm_dive] Dive {} confirmed by user {}", diveId, buddyUserId)
            return DiveConfirmationDTO.from(buddy.dive, buddy)
        } catch(e: DiveNotFoundException) {
            logger.warn("\n\t[WARN] [dive_service][confirm_dive] User {} is not a buddy of dive {}", buddyUserId, diveId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [dive_service][confirm_dive] Error confirming dive {} by user {}: {}", diveId, buddyUserId, e.message)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun listPhotos(ownerId: UUID, diveId: UUID): List<UploadedFileDTO> {
        findOwnedDive(ownerId, diveId)
        return uploadedFileService.listFiles(FileOwnerType.DIVE_PHOTO, diveId)
    }

    @Transactional
    fun addPhoto(ownerId: UUID, diveId: UUID, file: MultipartFile): UploadedFileDTO {
        findOwnedDive(ownerId, diveId)

        if(uploadedFileService.countFiles(FileOwnerType.DIVE_PHOTO, diveId) >= MAX_PHOTOS_PER_DIVE) {
            throw TooManyFilesException(MAX_PHOTOS_PER_DIVE)
        }

        return uploadedFileService.uploadFile(ownerId, FileOwnerType.DIVE_PHOTO, diveId, file)
    }

    @Transactional
    fun deletePhoto(ownerId: UUID, diveId: UUID, fileId: UUID) {
        findOwnedDive(ownerId, diveId)
        uploadedFileService.deleteResourceFile(ownerId, FileOwnerType.DIVE_PHOTO, diveId, fileId)
    }

    private fun validateRequest(request: DiveRequestDTO): DiveTimes {
        val errors = mutableListOf<String>()

        val zoneId = try {
            ZoneId.of(request.timeZone.trim())
        } catch(_: DateTimeException) {
            errors += "timeZone: invalid time zone '${request.timeZone}'"
            null
        }

        val times = zoneId?.let {
            DiveTimes(
                startedAt = request.startLocal.atZone(it).toInstant(),
                endedAt = request.endLocal.atZone(it).toInstant(),
                zoneId = it,
            )
        }

        if(times != null) {
            val duration = Duration.between(times.startedAt, times.endedAt)

            when {
                !times.endedAt.isAfter(times.startedAt) ->
                    errors += "endLocal: must be after startLocal"
                duration > MAX_DIVE_DURATION ->
                    errors += "endLocal: a dive cannot last more than ${MAX_DIVE_DURATION.toHours()} hours"
            }

            if(times.startedAt.isAfter(Instant.now().plus(FUTURE_TOLERANCE))) {
                errors += "startLocal: must not be in the future"
            }
        }

        request.avgDepthM?.let {
            if(it > request.maxDepthM) errors += "avgDepthM: must not exceed maxDepthM"
        }

        request.tanks.forEachIndexed { index, tank ->
            if(tank.endPressureBar > tank.startPressureBar) {
                errors += "tanks[$index].endPressureBar: must not exceed startPressureBar"
            }
            if(tank.o2Percent + tank.hePercent > 100.0) {
                errors += "tanks[$index]: o2Percent + hePercent must not exceed 100"
            }
        }

        validateSegments(request, times, errors)

        if(request.gear.mapNotNull { it.gearId }.hasDuplicates()) {
            errors += "gear: the same gear is selected more than once"
        }

        if(request.gear.mapNotNull { it.diveGearId }.hasDuplicates()) {
            errors += "gear: the same dive gear entry is selected more than once"
        }

        if(request.buddies.mapNotNull { it.userId }.hasDuplicates()) {
            errors += "buddies: the same user is added more than once"
        }

        if(request.buddies.mapNotNull { it.diveBuddyId }.hasDuplicates()) {
            errors += "buddies: the same buddy entry is selected more than once"
        }

        if(errors.isNotEmpty()) {
            throw InvalidDiveException(errors.joinToString("; "))
        }

        return times!!
    }

    private fun validateSegments(request: DiveRequestDTO, times: DiveTimes?, errors: MutableList<String>) {
        if(request.segments.isEmpty()) return

        request.segments.forEachIndexed { index, segment ->
            if(segment.depthM > request.maxDepthM + DEPTH_TOLERANCE_M) {
                errors += "segments[$index].depthM: must not exceed maxDepthM"
            }

            when(segment.type) {
                DiveSegmentType.SURFACE ->
                    if(segment.depthM != 0.0) errors += "segments[$index].depthM: must be 0 for SURFACE"
                DiveSegmentType.ASCENT ->
                    Unit
                else ->
                    if(segment.depthM <= 0.0) errors += "segments[$index].depthM: must be greater than 0 for ${segment.type}"
            }
        }

        if(times != null && times.endedAt.isAfter(times.startedAt)) {
            val segmentsSeconds = request.segments.sumOf { it.durationSeconds.toLong() }
            val diveSeconds = Duration.between(times.startedAt, times.endedAt).seconds

            if(abs(segmentsSeconds - diveSeconds) > SEGMENTS_DURATION_TOLERANCE_SECONDS) {
                errors += "segments: total duration (${segmentsSeconds / 60} min) does not match the dive duration (${diveSeconds / 60} min)"
            }
        }
    }

    private fun ensureNoOverlap(ownerId: UUID, times: DiveTimes, excludeDiveId: UUID?) {
        if(diveRepository.existsOverlapping(ownerId, times.startedAt, times.endedAt, excludeDiveId)) {
            throw DiveOverlapException()
        }
    }

    private fun applyRequest(ownerId: UUID, dive: Dive, request: DiveRequestDTO): List<DiveBuddy> {
        dive.purpose = request.purpose
        dive.entryMethod = request.entryMethod

        dive.weather = request.weather
        dive.seaState = request.seaState
        dive.currentStrength = request.currentStrength
        dive.visibilityM = request.visibilityM
        dive.airTemperatureC = request.airTemperatureC
        dive.surfaceWaterTemperatureC = request.surfaceWaterTemperatureC
        dive.bottomWaterTemperatureC = request.bottomWaterTemperatureC
        dive.waterType = request.waterType

        dive.weightKg = request.weightKg

        dive.residualNitrogenStart = request.residualNitrogenStart.cleanOrNull()?.uppercase()
        dive.residualNitrogenEnd = request.residualNitrogenEnd.cleanOrNull()?.uppercase()
        dive.residualNitrogenRate = request.residualNitrogenRate

        dive.segments = request.segments.map { it.toEntity() }

        dive.avgDepthM = request.avgDepthM ?: averageDepthFromSegments(dive.segments)

        dive.symptomsNotes = request.symptomsNotes.cleanOrNull()
        dive.rating = request.rating
        dive.notes = request.notes.cleanOrNull()

        dive.tags.clear()
        dive.tags.addAll(request.tags)
        dive.symptomsBefore.clear()
        dive.symptomsBefore.addAll(request.symptomsBefore)
        dive.symptomsAfter.clear()
        dive.symptomsAfter.addAll(request.symptomsAfter)

        dive.replaceTanks(
            request.tanks.map { tank ->
                DiveTank(
                    dive = dive,
                    volumeL = tank.volumeL,
                    workingPressureBar = tank.workingPressureBar,
                    material = tank.material,
                    startPressureBar = tank.startPressureBar,
                    endPressureBar = tank.endPressureBar,
                    o2Percent = tank.o2Percent,
                    hePercent = tank.hePercent,
                )
            }
        )

        dive.replaceGear(resolveGear(ownerId, dive, request.gear))

        val (buddies, addedBuddies) = resolveBuddies(ownerId, dive, request.buddies)
        dive.replaceBuddies(buddies)

        computeConsumption(dive)

        return addedBuddies
    }

    private fun resolveGear(ownerId: UUID, dive: Dive, selections: List<DiveGearSelectionDTO>): List<DiveGear> {
        if(selections.isEmpty()) return emptyList()

        val errors = mutableListOf<String>()
        val existingEntries = dive.gear.associateBy { it.id }

        val requestedGearIds = selections.mapNotNull { it.gearId }
        val ownedGear = if(requestedGearIds.isEmpty()) {
            emptyMap()
        } else {
            gearRepository.findAllByIdInAndOwnerId(requestedGearIds, ownerId).associateBy { it.id!! }
        }

        val items = selections.mapIndexedNotNull { index, selection ->
            if(selection.diveGearId != null) {
                existingEntries[selection.diveGearId]
                    ?: run {
                        errors += "gear[$index].diveGearId: entry not found in this dive"
                        null
                    }
            } else {
                ownedGear[selection.gearId]?.let { DiveGear.snapshotOf(it, dive) }
                    ?: run {
                        errors += "gear[$index].gearId: gear not found"
                        null
                    }
            }
        }

        if(errors.isNotEmpty()) {
            throw InvalidDiveException(errors.joinToString("; "))
        }

        return items
    }

    private fun resolveBuddies(ownerId: UUID, dive: Dive, selections: List<DiveBuddySelectionDTO>): Pair<List<DiveBuddy>, List<DiveBuddy>> {
        if(selections.isEmpty()) return emptyList<DiveBuddy>() to emptyList()

        val errors = mutableListOf<String>()
        val existingById = dive.buddies.associateBy { it.id }
        val existingByUserId = dive.buddies.filter { it.isRealUser() }.associateBy { it.buddyUser!!.id }

        val requestedUserIds = selections.mapNotNull { it.userId }
        val activeUsers = if(requestedUserIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllById(requestedUserIds)
                .filter { it.accountStatus == AccountStatus.ACTIVE }
                .associateBy { it.id!! }
        }

        val added = mutableListOf<DiveBuddy>()
        val usedUserIds = mutableSetOf<UUID>()

        val items = selections.mapIndexedNotNull { index, selection ->
            when {
                selection.diveBuddyId != null -> {
                    val entry = existingById[selection.diveBuddyId]
                    if(entry == null) {
                        errors += "buddies[$index].diveBuddyId: entry not found in this dive"
                        return@mapIndexedNotNull null
                    }

                    entry.buddyUser?.id?.let { userId ->
                        if(!usedUserIds.add(userId)) {
                            errors += "buddies[$index]: this user is already listed"
                            return@mapIndexedNotNull null
                        }
                    }

                    entry.role = selection.role
                    entry
                }

                selection.userId != null -> {
                    val userId = selection.userId

                    if(userId == ownerId) {
                        errors += "buddies[$index].userId: you cannot add yourself as buddy"
                        return@mapIndexedNotNull null
                    }
                    if(!usedUserIds.add(userId)) {
                        errors += "buddies[$index]: this user is already listed"
                        return@mapIndexedNotNull null
                    }

                    val existing = existingByUserId[userId]
                    if(existing != null) {
                        existing.role = selection.role
                        existing
                    } else {
                        val user = activeUsers[userId]
                        if(user == null) {
                            errors += "buddies[$index].userId: user not found"
                            null
                        } else {
                            DiveBuddy.forUser(dive, user, selection.role).also { added += it }
                        }
                    }
                }

                else -> DiveBuddy.custom(
                    dive = dive,
                    firstName = selection.firstName!!.trim(),
                    lastName = selection.lastName.cleanOrNull(),
                    role = selection.role,
                )
            }
        }

        if(errors.isNotEmpty()) {
            throw InvalidDiveException(errors.joinToString("; "))
        }

        return items to added
    }

    private fun averageDepthFromSegments(segments: List<DiveSegment>): Double? {
        var previousDepth = 0.0
        var depthTimeArea = 0.0
        var totalSeconds = 0L

        for((type, depthM, durationSeconds) in segments) {
            val seconds = durationSeconds.toDouble()

            depthTimeArea += if(type in TRANSITION_SEGMENTS) {
                (previousDepth + depthM) / 2.0 * seconds
            } else {
                depthM * seconds
            }

            totalSeconds += durationSeconds
            previousDepth = depthM
        }

        return if(totalSeconds > 0) depthTimeArea / totalSeconds else null
    }

    private fun computeConsumption(dive: Dive) {
        if(dive.tanks.isEmpty()) {
            dive.gasUsedL = null
            dive.rmvLMin = null
            return
        }

        val gasUsedL = dive.tanks.sumOf { it.gasUsedL() }
        val durationMin = dive.duration().seconds / 60.0

        dive.gasUsedL = gasUsedL
        dive.rmvLMin = dive.avgDepthM?.let { DivePhysics.rmvLMin(gasUsedL, durationMin, it, dive.waterType) }
    }

    private fun publishBuddyNotifications(dive: Dive, buddies: List<DiveBuddy>, reason: BuddyNotificationReason) {
        val notifications = buddies.mapNotNull { buddy ->
            val user = buddy.buddyUser ?: return@mapNotNull null

            BuddyNotification(
                diveId = dive.id!!,
                toEmail = user.email,
                buddyFirstName = user.firstName,
                ownerFullName = "${dive.owner.firstName} ${dive.owner.lastName}",
                siteName = dive.site.name,
                diveStartLocal = dive.localStart().toLocalDateTime(),
                reason = reason,
            )
        }

        if(notifications.isNotEmpty()) {
            eventPublisher.publishEvent(BuddyNotificationsEvent(notifications))
        }
    }

    private fun toDTO(ownerId: UUID, dive: Dive): DiveDTO {
        val surfaceIntervalMinutes: Long? = diveRepository
            .findFirstByOwnerIdAndStartedAtLessThanOrderByStartedAtDesc(ownerId, dive.startedAt)
            .map { Duration.between(it.endedAt, dive.startedAt).toMinutes() }
            .orElse(null)

        return DiveDTO.from(dive, diveNumberFor(ownerId, dive.id!!), surfaceIntervalMinutes)
    }

    private fun diveNumberFor(ownerId: UUID, diveId: UUID): Long =
        diveRepository.findDiveNumber(ownerId, diveId) + diveNumberOffset(ownerId)

    private fun diveNumbersFor(ownerId: UUID, dives: List<Dive>): Map<UUID, Long> {
        if(dives.isEmpty()) return emptyMap()

        val offset = diveNumberOffset(ownerId)
        return diveRepository.findDiveNumbers(ownerId, dives.map { it.id!! })
            .associate { it.id to it.diveNumber + offset }
    }

    private fun diveNumberOffset(ownerId: UUID): Long =
        (userRepository.findDiveNumberOffset(ownerId) ?: 0).toLong()

    private fun findOwnedDive(ownerId: UUID, diveId: UUID): Dive =
        diveRepository.findByIdAndOwnerId(diveId, ownerId)
            .orElseThrow { DiveNotFoundException(diveId) }

    private fun findVisibleSite(ownerId: UUID, siteId: UUID): DiveSite =
        diveSiteRepository.findVisibleById(siteId, ownerId)
            .orElseThrow { DiveSiteNotFoundException(siteId) }

    private fun saveAndFlush(dive: Dive): Dive =
        try {
            diveRepository.saveAndFlush(dive)
        } catch(e: DataIntegrityViolationException) {
            if(e.mostSpecificCause.message?.contains(OVERLAP_CONSTRAINT) == true) {
                throw DiveOverlapException()
            }

            throw e
        }

    private fun <T> List<T>.hasDuplicates(): Boolean = size != toSet().size

    private fun String?.cleanOrNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}