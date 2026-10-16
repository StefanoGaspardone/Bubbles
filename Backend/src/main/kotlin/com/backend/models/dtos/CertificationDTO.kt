package com.backend.models.dtos

import com.backend.models.entities.Certification
import com.backend.models.enums.CertificationAgency
import com.backend.models.enums.CertificationType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

const val CERTIFICATION_EXPIRING_SOON_DAYS = 60L

@Schema(description = "Certification, card or diving document of the current user")
data class CertificationDTO(
    @field:Schema(description = "Certification id", example = "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f")
    val id: UUID,

    @field:Schema(description = "Training agency", example = "PADI")
    val agency: CertificationAgency,

    @field:Schema(description = "Agency name, only when agency is OTHER", example = "Scuola Sub Liguria", nullable = true)
    val agencyName: String?,

    @field:Schema(description = "Agency name to display: agencyName for OTHER, otherwise the agency code", example = "PADI")
    val displayAgency: String,

    @field:Schema(description = "Document type", example = "RECREATIONAL")
    val certificationType: CertificationType,

    @field:Schema(description = "Course or document name", example = "Advanced Open Water Diver")
    val courseName: String,

    @field:Schema(description = "Certification or card number", example = "2405AB1234", nullable = true)
    val certificationNumber: String?,

    @field:Schema(description = "Issue date", example = "2024-06-15")
    val issueDate: LocalDate,

    @field:Schema(description = "Expiry date, for documents that expire", example = "2026-06-15", nullable = true)
    val expiryDate: LocalDate?,

    @field:Schema(description = "Whether the document has expired", example = "false")
    val expired: Boolean,

    @field:Schema(description = "Whether the document expires within $CERTIFICATION_EXPIRING_SOON_DAYS days (and has not expired yet)", example = "false")
    val expiringSoon: Boolean,

    @field:Schema(description = "Instructor name", example = "Luca Bianchi", nullable = true)
    val instructorName: String?,

    @field:Schema(description = "Instructor number", example = "123456", nullable = true)
    val instructorNumber: String?,

    @field:Schema(description = "Dive center where the course was taken", example = "Portofino Divers", nullable = true)
    val diveCenter: String?,

    @field:Schema(description = "Free notes", nullable = true)
    val notes: String?,

    @field:Schema(description = "File id of the card front photo", nullable = true)
    val frontImageId: UUID?,

    @field:Schema(description = "File id of the card back photo", nullable = true)
    val backImageId: UUID?,

    @field:Schema(description = "File id of the official PDF", nullable = true)
    val documentId: UUID?,

    @field:Schema(description = "Creation timestamp")
    val createdAt: Instant,

    @field:Schema(description = "Last update timestamp")
    val updatedAt: Instant,
) {
    companion object {
        fun from(certification: Certification, today: LocalDate = LocalDate.now()) = CertificationDTO(
            id = certification.id!!,
            agency = certification.agency,
            agencyName = certification.agencyName,
            displayAgency = certification.displayAgency(),
            certificationType = certification.certificationType,
            courseName = certification.courseName,
            certificationNumber = certification.certificationNumber,
            issueDate = certification.issueDate,
            expiryDate = certification.expiryDate,
            expired = certification.isExpired(today),
            expiringSoon = certification.isExpiringWithin(CERTIFICATION_EXPIRING_SOON_DAYS, today),
            instructorName = certification.instructorName,
            instructorNumber = certification.instructorNumber,
            diveCenter = certification.diveCenter,
            notes = certification.notes,
            frontImageId = certification.frontImageId,
            backImageId = certification.backImageId,
            documentId = certification.documentId,
            createdAt = certification.createdAt,
            updatedAt = certification.updatedAt,
        )
    }
}

@Schema(
    description = "Payload to create or fully replace a certification. On update, optional fields sent as null are cleared. Attachments (card photos and PDF) are managed separately."
)
data class CertificationRequestDTO(
    @field:Schema(description = "Training agency", example = "PADI")
    val agency: CertificationAgency,

    @field:Schema(description = "Agency name: required when agency is OTHER, not allowed otherwise", example = "Scuola Sub Liguria", nullable = true)
    @field:Size(max = 100)
    val agencyName: String? = null,

    @field:Schema(description = "Document type", example = "RECREATIONAL")
    val certificationType: CertificationType,

    @field:Schema(description = "Course or document name", example = "Advanced Open Water Diver")
    @field:NotBlank
    @field:Size(max = 150)
    val courseName: String,

    @field:Schema(description = "Certification or card number", example = "2405AB1234", nullable = true)
    @field:Size(max = 100)
    val certificationNumber: String? = null,

    @field:Schema(description = "Issue date, not in the future", example = "2024-06-15")
    @field:PastOrPresent
    val issueDate: LocalDate,

    @field:Schema(description = "Expiry date, after the issue date", example = "2026-06-15", nullable = true)
    val expiryDate: LocalDate? = null,

    @field:Schema(description = "Instructor name", example = "Luca Bianchi", nullable = true)
    @field:Size(max = 150)
    val instructorName: String? = null,

    @field:Schema(description = "Instructor number", example = "123456", nullable = true)
    @field:Size(max = 50)
    val instructorNumber: String? = null,

    @field:Schema(description = "Dive center where the course was taken", example = "Portofino Divers", nullable = true)
    @field:Size(max = 150)
    val diveCenter: String? = null,

    @field:Schema(description = "Free notes", nullable = true)
    @field:Size(max = 1000)
    val notes: String? = null,
)