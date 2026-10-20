package com.backend.services

import com.backend.exceptions.GearNotFoundException
import com.backend.exceptions.InvalidGearAttributesException
import com.backend.exceptions.TooManyFilesException
import com.backend.models.dtos.GearDTO
import com.backend.models.dtos.GearRequestDTO
import com.backend.models.dtos.UploadedFileDTO
import com.backend.models.entities.Gear
import com.backend.models.enums.FileOwnerType
import com.backend.models.enums.GearCategory
import com.backend.repositories.GearRepository
import com.backend.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.UUID

@Service
class GearService(
    private val gearRepository: GearRepository,
    private val userRepository: UserRepository,
    private val uploadedFileService: UploadedFileService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private companion object {
        val SIZE = AttributeSpec.TextSpec(maxLength = 20)

        const val MAX_FILES_PER_GEAR = 20

        val ATTRIBUTE_SPECS: Map<GearCategory, Map<String, AttributeSpec>> = mapOf(
            GearCategory.WETSUIT to mapOf(
                "thicknessMm" to AttributeSpec.NumberSpec(0.5, 15.0),
                "style" to AttributeSpec.EnumSpec(setOf("FULL", "SHORTY")),
            ),
            GearCategory.SEMIDRYSUIT to mapOf(
                "thicknessMm" to AttributeSpec.NumberSpec(3.0, 10.0),
            ),
            GearCategory.DRYSUIT to mapOf(
                "material" to AttributeSpec.EnumSpec(setOf("TRILAMINATE", "NEOPRENE")),
            ),
            GearCategory.WEIGHTS to mapOf(
                "kg" to AttributeSpec.NumberSpec(0.0, 40.0),
            ),
            GearCategory.REGULATOR to mapOf(
                "stage" to AttributeSpec.EnumSpec(setOf("FIRST", "SECOND", "SET")),
            ),
            GearCategory.BCD to mapOf("size" to SIZE),
            GearCategory.FINS to mapOf("size" to SIZE),
            GearCategory.MASK to mapOf("size" to SIZE),
            GearCategory.GLOVES to mapOf("size" to SIZE),
            GearCategory.BOOTS to mapOf("size" to SIZE),
        )
    }

    @Transactional(readOnly = true)
    fun listGear(ownerId: UUID, category: GearCategory?, includeArchived: Boolean): List<GearDTO> {
        logger.debug("\n\t[DEBUG] [gear_service][list_gear] Listing gear for user {} (category={}, includeArchived={})", ownerId, category, includeArchived)

        try {
            val gear = gearRepository.search(ownerId, category, includeArchived)

            logger.debug("\n\t[DEBUG] [gear_service][list_gear] Found {} items for user {}", gear.size, ownerId)
            return gear.map(GearDTO::from)
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [gear_service][list_gear] Error listing gear for user {}: {}", ownerId, e.message)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun getGear(ownerId: UUID, gearId: UUID): GearDTO {
        logger.debug("\n\t[DEBUG] [gear_service][get_gear] Retrieving gear {} for user {}", gearId, ownerId)

        try {
            return GearDTO.from(findOwnedGear(ownerId, gearId))
        } catch(e: GearNotFoundException) {
            logger.warn("\n\t[WARN] [gear_service][get_gear] Gear {} not found for user {}", gearId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [gear_service][get_gear] Error retrieving gear {} for user {}: {}", gearId, ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun createGear(ownerId: UUID, request: GearRequestDTO): GearDTO {
        logger.debug("\n\t[DEBUG] [gear_service][create_gear] Creating {} gear for user {}", request.category, ownerId)

        try {
            val attributes = validateAttributes(request.category, request.attributes)

            val gear = gearRepository.save(
                Gear(
                    owner = userRepository.getReferenceById(ownerId),
                    category = request.category,
                    name = request.name.trim(),
                    brand = request.brand.cleanOrNull(),
                    model = request.model.cleanOrNull(),
                    serialNumber = request.serialNumber.cleanOrNull(),
                    purchaseDate = request.purchaseDate,
                    lastServiceDate = request.lastServiceDate,
                    serviceIntervalMonths = request.serviceIntervalMonths,
                    notes = request.notes.cleanOrNull(),
                    attributes = attributes,
                )
            )

            logger.info("\n\t[INFO] [gear_service][create_gear] Gear {} created for user {}", gear.id, ownerId)
            return GearDTO.from(gear)
        } catch(e: InvalidGearAttributesException) {
            logger.warn("\n\t[WARN] [gear_service][create_gear] Invalid attributes for user {}: {}", ownerId, e.message)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [gear_service][create_gear] Error creating gear for user {}: {}", ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun updateGear(ownerId: UUID, gearId: UUID, request: GearRequestDTO): GearDTO {
        logger.debug("\n\t[DEBUG] [gear_service][update_gear] Updating gear {} for user {}", gearId, ownerId)

        try {
            val gear = findOwnedGear(ownerId, gearId)
            val attributes = validateAttributes(request.category, request.attributes)

            gear.category = request.category
            gear.name = request.name.trim()
            gear.brand = request.brand.cleanOrNull()
            gear.model = request.model.cleanOrNull()
            gear.serialNumber = request.serialNumber.cleanOrNull()
            gear.purchaseDate = request.purchaseDate
            gear.lastServiceDate = request.lastServiceDate
            gear.serviceIntervalMonths = request.serviceIntervalMonths
            gear.notes = request.notes.cleanOrNull()
            gear.attributes = attributes

            logger.info("\n\t[INFO] [gear_service][update_gear] Gear {} updated for user {}", gearId, ownerId)
            return GearDTO.from(gear)
        } catch(e: GearNotFoundException) {
            logger.warn("\n\t[WARN] [gear_service][update_gear] Gear {} not found for user {}", gearId, ownerId)
            throw e
        } catch(e: InvalidGearAttributesException) {
            logger.warn("\n\t[WARN] [gear_service][update_gear] Invalid attributes for gear {}: {}", gearId, e.message)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [gear_service][update_gear] Error updating gear {} for user {}: {}", gearId, ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun archiveGear(ownerId: UUID, gearId: UUID): GearDTO {
        logger.debug("\n\t[DEBUG] [gear_service][archive_gear] Archiving gear {} for user {}", gearId, ownerId)

        try {
            val gear = findOwnedGear(ownerId, gearId)
            gear.archive(Instant.now())

            logger.info("\n\t[INFO] [gear_service][archive_gear] Gear {} archived for user {}", gearId, ownerId)
            return GearDTO.from(gear)
        } catch(e: GearNotFoundException) {
            logger.warn("\n\t[WARN] [gear_service][archive_gear] Gear {} not found for user {}", gearId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [gear_service][archive_gear] Error archiving gear {} for user {}: {}", gearId, ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun restoreGear(ownerId: UUID, gearId: UUID): GearDTO {
        logger.debug("\n\t[DEBUG] [gear_service][restore_gear] Restoring gear {} for user {}", gearId, ownerId)

        try {
            val gear = findOwnedGear(ownerId, gearId)
            gear.restore()

            logger.info("\n\t[INFO] [gear_service][restore_gear] Gear {} restored for user {}", gearId, ownerId)
            return GearDTO.from(gear)
        } catch(e: GearNotFoundException) {
            logger.warn("\n\t[WARN] [gear_service][restore_gear] Gear {} not found for user {}", gearId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [gear_service][restore_gear] Error restoring gear {} for user {}: {}", gearId, ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun deleteGear(ownerId: UUID, gearId: UUID) {
        logger.debug("\n\t[DEBUG] [gear_service][delete_gear] Deleting gear {} for user {}", gearId, ownerId)

        try {
            val gear = findOwnedGear(ownerId, gearId)

            uploadedFileService.deleteAllForOwner(FileOwnerType.GEAR, gearId)
            gearRepository.delete(gear)

            logger.info("\n\t[INFO] [gear_service][delete_gear] Gear {} deleted for user {}", gearId, ownerId)
        } catch(e: GearNotFoundException) {
            logger.warn("\n\t[WARN] [gear_service][delete_gear] Gear {} not found for user {}", gearId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [gear_service][delete_gear] Error deleting gear {} for user {}: {}", gearId, ownerId, e.message)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun listFiles(ownerId: UUID, gearId: UUID): List<UploadedFileDTO> {
        findOwnedGear(ownerId, gearId)
        return uploadedFileService.listFiles(FileOwnerType.GEAR, gearId)
    }

    @Transactional
    fun addFile(ownerId: UUID, gearId: UUID, file: MultipartFile): UploadedFileDTO {
        findOwnedGear(ownerId, gearId)

        if(uploadedFileService.countFiles(FileOwnerType.GEAR, gearId) >= MAX_FILES_PER_GEAR) {
            throw TooManyFilesException(MAX_FILES_PER_GEAR)
        }

        return uploadedFileService.uploadFile(ownerId, FileOwnerType.GEAR, gearId, file)
    }

    @Transactional
    fun deleteFile(ownerId: UUID, gearId: UUID, fileId: UUID) {
        findOwnedGear(ownerId, gearId)
        uploadedFileService.deleteResourceFile(ownerId, FileOwnerType.GEAR, gearId, fileId)
    }

    private sealed interface AttributeSpec {
        data class NumberSpec(val min: Double, val max: Double) : AttributeSpec
        data class EnumSpec(val allowed: Set<String>) : AttributeSpec
        data class TextSpec(val maxLength: Int) : AttributeSpec
    }

    private fun validateAttributes(category: GearCategory, attributes: Map<String, Any?>): Map<String, Any?> {
        val specs = ATTRIBUTE_SPECS[category].orEmpty()
        val errors = mutableListOf<String>()
        val normalized = linkedMapOf<String, Any?>()

        for((key, value) in attributes) {
            if(value == null) continue

            val spec = specs[key]
            if(spec == null) {
                errors += "$key: not allowed for category $category"
                continue
            }

            when(spec) {
                is AttributeSpec.NumberSpec -> {
                    val number = (value as? Number)?.toDouble()
                    when {
                        number == null || !number.isFinite() ->
                            errors += "$key: must be a number"
                        number < spec.min || number > spec.max ->
                            errors += "$key: must be between ${formatNumber(spec.min)} and ${formatNumber(spec.max)}"
                        else ->
                            normalized[key] = compactNumber(number)
                    }
                }

                is AttributeSpec.EnumSpec -> {
                    val text = (value as? String)?.trim()?.uppercase()
                    if(text == null || text !in spec.allowed) {
                        errors += "$key: must be one of ${spec.allowed.joinToString(", ")}"
                    } else {
                        normalized[key] = text
                    }
                }

                is AttributeSpec.TextSpec -> {
                    val text = (value as? String)?.trim()
                    when {
                        text == null -> errors += "$key: must be a string"
                        text.isEmpty() -> Unit
                        text.length > spec.maxLength -> errors += "$key: must be at most ${spec.maxLength} characters"
                        else -> normalized[key] = text
                    }
                }
            }
        }

        if(errors.isNotEmpty()) {
            throw InvalidGearAttributesException(errors.joinToString("; ") { "attributes.$it" })
        }

        return normalized
    }

    private fun compactNumber(value: Double): Number =
        if(value % 1.0 == 0.0) value.toLong() else value

    private fun formatNumber(value: Double): String =
        compactNumber(value).toString()

    private fun findOwnedGear(ownerId: UUID, gearId: UUID): Gear =
        gearRepository.findByIdAndOwnerId(gearId, ownerId)
            .orElseThrow { GearNotFoundException(gearId) }

    private fun String?.cleanOrNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}