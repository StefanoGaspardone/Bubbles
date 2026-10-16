package com.backend.services

import com.backend.exceptions.CertificationNotFoundException
import com.backend.exceptions.InvalidCertificationException
import com.backend.models.dtos.CertificationDTO
import com.backend.models.dtos.CertificationRequestDTO
import com.backend.models.entities.Certification
import com.backend.models.enums.CertificationAgency
import com.backend.models.enums.CertificationType
import com.backend.repositories.CertificationRepository
import com.backend.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class CertificationService(
    private val certificationRepository: CertificationRepository,
    private val userRepository: UserRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun listCertifications(
        ownerId: UUID,
        agency: CertificationAgency?,
        certificationType: CertificationType?,
        includeExpired: Boolean,
    ): List<CertificationDTO> {
        logger.debug("\n\t[DEBUG] [certification_service][list_certifications] Listing certifications for user {} (agency={}, type={}, includeExpired={})", ownerId, agency, certificationType, includeExpired)

        try {
            val today = LocalDate.now()
            val certifications = certificationRepository.search(ownerId, agency, certificationType, includeExpired, today)

            logger.debug("\n\t[DEBUG] [certification_service][list_certifications] Found {} certifications for user {}", certifications.size, ownerId)
            return certifications.map { CertificationDTO.from(it, today) }
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [certification_service][list_certifications] Error listing certifications for user {}: {}", ownerId, e.message)
            throw e
        }
    }

    @Transactional(readOnly = true)
    fun getCertification(ownerId: UUID, certificationId: UUID): CertificationDTO {
        logger.debug("\n\t[DEBUG] [certification_service][get_certification] Retrieving certification {} for user {}", certificationId, ownerId)

        try {
            return CertificationDTO.from(findOwnedCertification(ownerId, certificationId))
        } catch(e: CertificationNotFoundException) {
            logger.warn("\n\t[WARN] [certification_service][get_certification] Certification {} not found for user {}", certificationId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [certification_service][get_certification] Error retrieving certification {} for user {}: {}", certificationId, ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun createCertification(ownerId: UUID, request: CertificationRequestDTO): CertificationDTO {
        logger.debug("\n\t[DEBUG] [certification_service][create_certification] Creating {} certification for user {}", request.agency, ownerId)

        try {
            validateRequest(request)

            val certification = certificationRepository.save(
                Certification(
                    owner = userRepository.getReferenceById(ownerId),
                    agency = request.agency,
                    agencyName = agencyNameFor(request),
                    certificationType = request.certificationType,
                    courseName = request.courseName.trim(),
                    certificationNumber = request.certificationNumber.cleanOrNull(),
                    issueDate = request.issueDate,
                    expiryDate = request.expiryDate,
                    instructorName = request.instructorName.cleanOrNull(),
                    instructorNumber = request.instructorNumber.cleanOrNull(),
                    diveCenter = request.diveCenter.cleanOrNull(),
                    notes = request.notes.cleanOrNull(),
                )
            )

            logger.info("\n\t[INFO] [certification_service][create_certification] Certification {} created for user {}", certification.id, ownerId)
            return CertificationDTO.from(certification)
        } catch(e: InvalidCertificationException) {
            logger.warn("\n\t[WARN] [certification_service][create_certification] Invalid certification for user {}: {}", ownerId, e.message)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [certification_service][create_certification] Error creating certification for user {}: {}", ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun updateCertification(ownerId: UUID, certificationId: UUID, request: CertificationRequestDTO): CertificationDTO {
        logger.debug("\n\t[DEBUG] [certification_service][update_certification] Updating certification {} for user {}", certificationId, ownerId)

        try {
            val certification = findOwnedCertification(ownerId, certificationId)
            validateRequest(request)

            certification.agency = request.agency
            certification.agencyName = agencyNameFor(request)
            certification.certificationType = request.certificationType
            certification.courseName = request.courseName.trim()
            certification.certificationNumber = request.certificationNumber.cleanOrNull()
            certification.issueDate = request.issueDate
            certification.expiryDate = request.expiryDate
            certification.instructorName = request.instructorName.cleanOrNull()
            certification.instructorNumber = request.instructorNumber.cleanOrNull()
            certification.diveCenter = request.diveCenter.cleanOrNull()
            certification.notes = request.notes.cleanOrNull()

            logger.info("\n\t[INFO] [certification_service][update_certification] Certification {} updated for user {}", certificationId, ownerId)
            return CertificationDTO.from(certification)
        } catch(e: CertificationNotFoundException) {
            logger.warn("\n\t[WARN] [certification_service][update_certification] Certification {} not found for user {}", certificationId, ownerId)
            throw e
        } catch(e: InvalidCertificationException) {
            logger.warn("\n\t[WARN] [certification_service][update_certification] Invalid certification {} for user {}: {}", certificationId, ownerId, e.message)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [certification_service][update_certification] Error updating certification {} for user {}: {}", certificationId, ownerId, e.message)
            throw e
        }
    }

    @Transactional
    fun deleteCertification(ownerId: UUID, certificationId: UUID) {
        logger.debug("\n\t[DEBUG] [certification_service][delete_certification] Deleting certification {} for user {}", certificationId, ownerId)

        try {
            val certification = findOwnedCertification(ownerId, certificationId)
            certificationRepository.delete(certification)

            logger.info("\n\t[INFO] [certification_service][delete_certification] Certification {} deleted for user {}", certificationId, ownerId)
        } catch(e: CertificationNotFoundException) {
            logger.warn("\n\t[WARN] [certification_service][delete_certification] Certification {} not found for user {}", certificationId, ownerId)
            throw e
        } catch(e: Exception) {
            logger.error("\n\t[ERROR] [certification_service][delete_certification] Error deleting certification {} for user {}: {}", certificationId, ownerId, e.message)
            throw e
        }
    }

    private fun validateRequest(request: CertificationRequestDTO) {
        val errors = mutableListOf<String>()
        val agencyName = request.agencyName.cleanOrNull()

        if(request.agency == CertificationAgency.OTHER && agencyName == null) {
            errors += "agencyName: is required when agency is OTHER"
        }
        if(request.agency != CertificationAgency.OTHER && agencyName != null) {
            errors += "agencyName: is allowed only when agency is OTHER"
        }

        if(request.expiryDate != null && !request.expiryDate.isAfter(request.issueDate)) {
            errors += "expiryDate: must be after issueDate"
        }

        if(errors.isNotEmpty()) {
            throw InvalidCertificationException(errors.joinToString("; "))
        }
    }

    private fun findOwnedCertification(ownerId: UUID, certificationId: UUID): Certification =
        certificationRepository.findByIdAndOwnerId(certificationId, ownerId)
            .orElseThrow { CertificationNotFoundException(certificationId) }

    private fun agencyNameFor(request: CertificationRequestDTO): String? =
        if(request.agency == CertificationAgency.OTHER) request.agencyName.cleanOrNull() else null

    private fun String?.cleanOrNull(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() }
}