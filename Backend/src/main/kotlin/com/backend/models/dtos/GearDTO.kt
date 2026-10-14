package com.backend.models.dtos

import com.backend.models.entities.Gear
import com.backend.models.enums.GearCategory
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private const val ATTRIBUTES_DESCRIPTION =
    "Category-specific attributes. Allowed keys depend on the category: " +
            "WETSUIT {thicknessMm: number, style: FULL|SHORTY}; " +
            "SEMIDRYSUIT {thicknessMm: number}; " +
            "DRYSUIT {material: TRILAMINATE|NEOPRENE}; " +
            "WEIGHTS {kg: number}; " +
            "REGULATOR {stage: FIRST|SECOND|SET}; " +
            "BCD, FINS, MASK, GLOVES, BOOTS {size: string}. " +
            "Other categories accept no attributes. Unknown keys are rejected."

@Schema(description = "A piece of diving gear owned by the current user")
data class GearDTO(
    @field:Schema(description = "Unique identifier of the gear", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    val id: UUID,

    @field:Schema(description = "Gear category", example = "WETSUIT")
    val category: GearCategory,

    @field:Schema(description = "User-defined name", example = "Winter wetsuit")
    val name: String,

    @field:Schema(description = "Brand", example = "Mares", nullable = true)
    val brand: String?,

    @field:Schema(description = "Model", example = "Flexa 5-4-3", nullable = true)
    val model: String?,

    @field:Schema(description = "Serial number", example = "MR-2024-00123", nullable = true)
    val serialNumber: String?,

    @field:Schema(description = "Purchase date", example = "2024-03-15", nullable = true)
    val purchaseDate: LocalDate?,

    @field:Schema(description = "Date of the last service", example = "2025-04-01", nullable = true)
    val lastServiceDate: LocalDate?,

    @field:Schema(description = "Recommended service interval in months", example = "12", nullable = true)
    val serviceIntervalMonths: Int?,

    @field:Schema(
        description = "Next service date, computed from the last service date (or purchase date) and the interval. Null if not enough data.",
        example = "2026-04-01",
        nullable = true,
    )
    val nextServiceDate: LocalDate?,

    @field:Schema(description = "Whether the next service date has been reached", example = "false")
    val serviceDue: Boolean,

    @field:Schema(description = "Free notes", example = "Small tear on the left knee", nullable = true)
    val notes: String?,

    @field:Schema(description = ATTRIBUTES_DESCRIPTION, example = "{\"thicknessMm\": 5, \"style\": \"FULL\"}")
    val attributes: Map<String, Any?>,

    @field:Schema(description = "Whether the gear is archived (sold, broken...). Archived gear is not proposed for new dives.", example = "false")
    val archived: Boolean,

    @field:Schema(description = "When the gear was archived. Null if in use.", nullable = true)
    val archivedAt: Instant?,

    @field:Schema(description = "Creation timestamp")
    val createdAt: Instant,

    @field:Schema(description = "Last update timestamp")
    val updatedAt: Instant,
) {
    companion object {
        fun from(gear: Gear) = GearDTO(
            id = gear.id!!,
            category = gear.category,
            name = gear.name,
            brand = gear.brand,
            model = gear.model,
            serialNumber = gear.serialNumber,
            purchaseDate = gear.purchaseDate,
            lastServiceDate = gear.lastServiceDate,
            serviceIntervalMonths = gear.serviceIntervalMonths,
            nextServiceDate = gear.nextServiceDate(),
            serviceDue = gear.isServiceDue(),
            notes = gear.notes,
            attributes = gear.attributes,
            archived = gear.isArchived(),
            archivedAt = gear.archivedAt,
            createdAt = gear.createdAt,
            updatedAt = gear.updatedAt,
        )
    }
}

@Schema(description = "Payload to create or fully replace a piece of gear. On update, optional fields sent as null are cleared.")
data class GearRequestDTO(
    @field:Schema(description = "Gear category", example = "WETSUIT")
    val category: GearCategory,

    @field:Schema(description = "User-defined name", example = "Winter wetsuit")
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Schema(description = "Brand", example = "Mares", nullable = true)
    @field:Size(max = 100)
    val brand: String? = null,

    @field:Schema(description = "Model", example = "Flexa 5-4-3", nullable = true)
    @field:Size(max = 100)
    val model: String? = null,

    @field:Schema(description = "Serial number", example = "MR-2024-00123", nullable = true)
    @field:Size(max = 100)
    val serialNumber: String? = null,

    @field:Schema(description = "Purchase date, not in the future", example = "2024-03-15", nullable = true)
    @field:PastOrPresent
    val purchaseDate: LocalDate? = null,

    @field:Schema(description = "Date of the last service, not in the future", example = "2025-04-01", nullable = true)
    @field:PastOrPresent
    val lastServiceDate: LocalDate? = null,

    @field:Schema(description = "Recommended service interval in months (1-120)", example = "12", nullable = true)
    @field:Min(1)
    @field:Max(120)
    val serviceIntervalMonths: Int? = null,

    @field:Schema(description = "Free notes", example = "Small tear on the left knee", nullable = true)
    @field:Size(max = 1000)
    val notes: String? = null,

    @field:Schema(description = ATTRIBUTES_DESCRIPTION, example = "{\"thicknessMm\": 5, \"style\": \"FULL\"}")
    @field:Size(max = 20)
    val attributes: Map<String, Any?> = emptyMap(),
)