package com.backend.models.dtos

import com.backend.models.entities.DiveSite
import com.backend.models.enums.DiveSiteSource
import com.backend.models.enums.DiveSiteType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Schema(description = "Dive site visible to the current user: one of their own sites or a public one")
data class DiveSiteDTO(
    @field:Schema(description = "Dive site id", example = "0b8a3c1e-1f2d-4c3b-9a8e-7d6f5e4c3b2a")
    val id: UUID,

    @field:Schema(description = "Site name", example = "Secca dell'Isuela")
    val name: String,

    @field:Schema(description = "Locality", example = "Portofino", nullable = true)
    val locality: String?,

    @field:Schema(description = "ISO 3166-1 alpha-2 country code", example = "IT", nullable = true)
    val countryCode: String?,

    @field:Schema(description = "Latitude in decimal degrees", example = "44.3036")
    val latitude: Double,

    @field:Schema(description = "Longitude in decimal degrees", example = "9.2125")
    val longitude: Double,

    @field:Schema(description = "Site type", example = "SHOAL", nullable = true)
    val siteType: DiveSiteType?,

    @field:Schema(description = "Indicative minimum depth in meters", example = "12", nullable = true)
    val minDepthM: Double?,

    @field:Schema(description = "Indicative maximum depth in meters", example = "40", nullable = true)
    val maxDepthM: Double?,

    @field:Schema(description = "Free description", nullable = true)
    val description: String?,

    @field:Schema(description = "Where the site comes from", example = "USER")
    val source: DiveSiteSource,

    @field:Schema(description = "Whether the site is public (imported) rather than private to the user", example = "false")
    val isPublic: Boolean,

    @field:Schema(description = "Whether the current user can modify or delete the site (only their own sites)", example = "true")
    val editable: Boolean,

    @field:Schema(description = "Number of the current user's dives at this site", example = "7")
    val diveCount: Long,

    @field:Schema(description = "Creation timestamp")
    val createdAt: Instant,

    @field:Schema(description = "Last update timestamp")
    val updatedAt: Instant,
) {
    companion object {
        fun from(site: DiveSite, diveCount: Long) = DiveSiteDTO(
            id = site.id!!,
            name = site.name,
            locality = site.locality,
            countryCode = site.countryCode,
            latitude = site.latitude,
            longitude = site.longitude,
            siteType = site.siteType,
            minDepthM = site.minDepthM,
            maxDepthM = site.maxDepthM,
            description = site.description,
            source = site.source,
            isPublic = site.isPublic(),
            editable = !site.isPublic(),
            diveCount = diveCount,
            createdAt = site.createdAt,
            updatedAt = site.updatedAt,
        )
    }
}

@Schema(description = "Payload to create or fully replace a personal dive site. On update, optional fields sent as null are cleared.")
data class DiveSiteRequestDTO(
    @field:Schema(description = "Site name", example = "Secca dell'Isuela")
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,

    @field:Schema(description = "Locality", example = "Portofino", nullable = true)
    @field:Size(max = 150)
    val locality: String? = null,

    @field:Schema(description = "ISO 3166-1 alpha-2 country code, case-insensitive", example = "IT", nullable = true)
    @field:Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a 2-letter ISO country code")
    val countryCode: String? = null,

    @field:Schema(description = "Latitude in decimal degrees (-90 to 90)", example = "44.3036")
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double,

    @field:Schema(description = "Longitude in decimal degrees (-180 to 180)", example = "9.2125")
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double,

    @field:Schema(description = "Site type", example = "SHOAL", nullable = true)
    val siteType: DiveSiteType? = null,

    @field:Schema(description = "Indicative minimum depth in meters", example = "12", nullable = true)
    @field:DecimalMin("0.0")
    @field:DecimalMax("350.0")
    val minDepthM: Double? = null,

    @field:Schema(description = "Indicative maximum depth in meters, not less than minDepthM", example = "40", nullable = true)
    @field:DecimalMin(value = "0.0", inclusive = false)
    @field:DecimalMax("350.0")
    val maxDepthM: Double? = null,

    @field:Schema(description = "Free description", nullable = true)
    @field:Size(max = 2000)
    val description: String? = null,
)