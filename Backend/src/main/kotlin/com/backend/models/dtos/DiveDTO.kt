package com.backend.models.dtos

import com.backend.models.entities.Dive
import com.backend.models.entities.DiveGear
import com.backend.models.entities.DiveSegment
import com.backend.models.entities.DiveSite
import com.backend.models.entities.DiveTank
import com.backend.models.enums.CurrentStrength
import com.backend.models.enums.DivePurpose
import com.backend.models.enums.DiveSegmentType
import com.backend.models.enums.DiveSiteType
import com.backend.models.enums.DiveSymptom
import com.backend.models.enums.DiveTag
import com.backend.models.enums.EntryMethod
import com.backend.models.enums.GearCategory
import com.backend.models.enums.SeaState
import com.backend.models.enums.TankMaterial
import com.backend.models.enums.WaterType
import com.backend.models.enums.WeatherCondition
import com.backend.utils.DivePhysics
import com.backend.utils.roundTo
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Phase of the dive profile")
data class DiveSegmentDTO(
    @field:Schema(description = "Phase type", example = "BOTTOM")
    val type: DiveSegmentType,

    @field:Schema(
        description = "Depth in meters. For DESCENT/ASCENT: depth reached at the end of the phase. For BOTTOM and stops: depth held during the phase. For SURFACE: 0.",
        example = "18",
    )
    @field:DecimalMin("0.0")
    @field:DecimalMax("350.0")
    val depthM: Double,

    @field:Schema(description = "Duration of the phase in seconds", example = "1500")
    @field:Min(1)
    @field:Max(36_000)
    val durationSeconds: Int,
) {
    fun toEntity() =
        DiveSegment(type = type, depthM = depthM, durationSeconds = durationSeconds)

    companion object {
        fun from(segment: DiveSegment) = DiveSegmentDTO(
            type = segment.type,
            depthM = segment.depthM,
            durationSeconds = segment.durationSeconds,
        )
    }
}

@Schema(description = "Summary of a dive site, as shown inside a dive")
data class DiveSiteSummaryDTO(
    @field:Schema(description = "Dive site id", example = "0b8a3c1e-1f2d-4c3b-9a8e-7d6f5e4c3b2a")
    val id: UUID,

    @field:Schema(description = "Site name", example = "Secca dell'Isuela")
    val name: String,

    @field:Schema(description = "Locality", example = "Portofino", nullable = true)
    val locality: String?,

    @field:Schema(description = "ISO 3166-1 alpha-2 country code", example = "IT", nullable = true)
    val countryCode: String?,

    @field:Schema(description = "Latitude", example = "44.3036")
    val latitude: Double,

    @field:Schema(description = "Longitude", example = "9.2125")
    val longitude: Double,

    @field:Schema(description = "Site type", example = "SHOAL", nullable = true)
    val siteType: DiveSiteType?,

    @field:Schema(description = "Whether the site is public (imported) or private to the user", example = "false")
    val isPublic: Boolean,
) {
    companion object {
        fun from(site: DiveSite) = DiveSiteSummaryDTO(
            id = site.id!!,
            name = site.name,
            locality = site.locality,
            countryCode = site.countryCode,
            latitude = site.latitude,
            longitude = site.longitude,
            siteType = site.siteType,
            isPublic = site.isPublic(),
        )
    }
}

@Schema(description = "Tank used in the dive, with gas mix and pressures")
data class DiveTankRequestDTO(
    @field:Schema(description = "Tank volume in liters (water capacity)", example = "12")
    @field:DecimalMin("0.5")
    @field:DecimalMax("30.0")
    val volumeL: Double,

    @field:Schema(description = "Working pressure in bar", example = "232", nullable = true)
    @field:Min(100)
    @field:Max(350)
    val workingPressureBar: Int? = null,

    @field:Schema(description = "Tank material", example = "STEEL", nullable = true)
    val material: TankMaterial? = null,

    @field:Schema(description = "Pressure at the start of the dive, in bar", example = "200")
    @field:Min(1)
    @field:Max(350)
    val startPressureBar: Int,

    @field:Schema(description = "Pressure at the end of the dive, in bar. Must not exceed the start pressure.", example = "60")
    @field:Min(0)
    @field:Max(350)
    val endPressureBar: Int,

    @field:Schema(description = "Oxygen percentage: 21 for air, 22-40 for recreational Nitrox", example = "32")
    @field:DecimalMin("21.0")
    @field:DecimalMax("100.0")
    val o2Percent: Double = 21.0,

    @field:Schema(description = "Helium percentage (0 unless trimix)", example = "0")
    @field:DecimalMin("0.0")
    @field:DecimalMax("95.0")
    val hePercent: Double = 0.0,
)

@Schema(description = "Gear used in the dive. Set exactly one of diveGearId (keep an existing entry of this dive unchanged, only on update) or gearId (snapshot a piece of the user's gear).")
data class DiveGearSelectionDTO(
    @field:Schema(description = "Id of a gear entry already saved in this dive", nullable = true)
    val diveGearId: UUID? = null,

    @field:Schema(description = "Id of a piece of the user's gear to snapshot", nullable = true)
    val gearId: UUID? = null,
) {
    @get:JsonIgnore
    @get:Schema(hidden = true)
    @get:AssertTrue(message = "exactly one of diveGearId and gearId must be set")
    val isValidSelection: Boolean
        get() = (diveGearId == null) != (gearId == null)
}

@Schema(description = "Payload to create or fully replace a dive. On update, optional fields sent as null are cleared and lists are replaced.")
data class DiveRequestDTO(
    @field:Schema(description = "Dive site id (public or owned by the user)", example = "0b8a3c1e-1f2d-4c3b-9a8e-7d6f5e4c3b2a")
    val siteId: UUID,

    @field:Schema(description = "Local date and time of entry into the water, at the dive site", example = "2026-08-10T09:30:00")
    val startLocal: LocalDateTime,

    @field:Schema(description = "Local date and time of exit from the water, at the dive site", example = "2026-08-10T10:22:00")
    val endLocal: LocalDateTime,

    @field:Schema(description = "IANA time zone of the dive site", example = "Europe/Rome")
    @field:NotBlank
    @field:Size(max = 64)
    val timeZone: String,

    @field:Schema(description = "Main purpose of the dive", example = "RECREATIONAL", nullable = true)
    val purpose: DivePurpose? = null,

    @field:Schema(description = "Entry method", example = "BOAT", nullable = true)
    val entryMethod: EntryMethod? = null,

    @field:Schema(description = "Dive characteristics (multiple choice)", example = "[\"WALL\", \"DRIFT\"]")
    @field:Size(max = 10)
    val tags: Set<DiveTag> = emptySet(),

    @field:Schema(description = "Weather", example = "SUNNY", nullable = true)
    val weather: WeatherCondition? = null,

    @field:Schema(description = "Sea state", example = "CALM", nullable = true)
    val seaState: SeaState? = null,

    @field:Schema(description = "Current strength", example = "LIGHT", nullable = true)
    val currentStrength: CurrentStrength? = null,

    @field:Schema(description = "Visibility in meters", example = "15", nullable = true)
    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val visibilityM: Double? = null,

    @field:Schema(description = "Air temperature in °C", example = "28", nullable = true)
    @field:DecimalMin("-50.0")
    @field:DecimalMax("60.0")
    val airTemperatureC: Double? = null,

    @field:Schema(description = "Water temperature at the surface in °C", example = "24", nullable = true)
    @field:DecimalMin("-3.0")
    @field:DecimalMax("40.0")
    val surfaceWaterTemperatureC: Double? = null,

    @field:Schema(description = "Water temperature at the bottom in °C", example = "18", nullable = true)
    @field:DecimalMin("-3.0")
    @field:DecimalMax("40.0")
    val bottomWaterTemperatureC: Double? = null,

    @field:Schema(description = "Water type, affects depth/pressure conversion", example = "SALT")
    val waterType: WaterType = WaterType.SALT,

    @field:Schema(description = "Maximum depth in meters", example = "24.5")
    @field:DecimalMin(value = "0.0", inclusive = false)
    @field:DecimalMax("350.0")
    val maxDepthM: Double,

    @field:Schema(
        description = "Average depth in meters, needed for SAC/RMV. If null and a profile is given, it is computed from the profile.",
        example = "14.2",
        nullable = true,
    )
    @field:DecimalMin(value = "0.0", inclusive = false)
    @field:DecimalMax("350.0")
    val avgDepthM: Double? = null,

    @field:Schema(description = "Weight carried in kg", example = "6", nullable = true)
    @field:DecimalMin("0.0")
    @field:DecimalMax("40.0")
    val weightKg: Double? = null,

    @field:Schema(description = "Residual nitrogen factor/group at the start, as given by the tables used", example = "C", nullable = true)
    @field:Size(max = 10)
    val residualNitrogenStart: String? = null,

    @field:Schema(description = "Residual nitrogen factor/group at the end, as given by the tables used", example = "F", nullable = true)
    @field:Size(max = 10)
    val residualNitrogenEnd: String? = null,

    @field:Schema(description = "Residual nitrogen rate (TAR), as given by the tables used", example = "1.2", nullable = true)
    @field:DecimalMin("0.0")
    val residualNitrogenRate: Double? = null,

    @field:Schema(description = "Profile phases in chronological order")
    @field:Valid
    @field:Size(max = 30)
    val segments: List<DiveSegmentDTO> = emptyList(),

    @field:Schema(description = "Tanks used, the first is the main one")
    @field:Valid
    @field:Size(max = 4)
    val tanks: List<DiveTankRequestDTO> = emptyList(),

    @field:Schema(description = "Gear used, in display order")
    @field:Valid
    @field:Size(max = 40)
    val gear: List<DiveGearSelectionDTO> = emptyList(),

    @field:Schema(
        description = "Buddies of the dive, in display order. Real users (userId) receive an email and can confirm the dive by scanning its QR code; custom buddies (firstName/lastName) need no confirmation.",
    )
    @field:Valid
    @field:Size(max = 10)
    val buddies: List<DiveBuddySelectionDTO> = emptyList(),

    @field:Schema(description = "Symptoms before the dive", example = "[\"FATIGUE\"]")
    val symptomsBefore: Set<DiveSymptom> = emptySet(),

    @field:Schema(description = "Symptoms after the dive", example = "[]")
    val symptomsAfter: Set<DiveSymptom> = emptySet(),

    @field:Schema(description = "Free notes about symptoms", nullable = true)
    @field:Size(max = 1000)
    val symptomsNotes: String? = null,

    @field:Schema(description = "Personal rating from 1 to 5", example = "4", nullable = true)
    @field:Min(1)
    @field:Max(5)
    val rating: Int? = null,

    @field:Schema(description = "Free notes", nullable = true)
    @field:Size(max = 4000)
    val notes: String? = null,
)

@Schema(description = "Tank used in a dive, with computed consumption")
data class DiveTankDTO(
    @field:Schema(description = "Tank id")
    val id: UUID,

    @field:Schema(description = "Position in the dive (0 = main tank)", example = "0")
    val position: Int,

    @field:Schema(description = "Tank volume in liters", example = "12")
    val volumeL: Double,

    @field:Schema(description = "Working pressure in bar", example = "232", nullable = true)
    val workingPressureBar: Int?,

    @field:Schema(description = "Tank material", example = "STEEL", nullable = true)
    val material: TankMaterial?,

    @field:Schema(description = "Start pressure in bar", example = "200")
    val startPressureBar: Int,

    @field:Schema(description = "End pressure in bar", example = "60")
    val endPressureBar: Int,

    @field:Schema(description = "Oxygen percentage", example = "32")
    val o2Percent: Double,

    @field:Schema(description = "Helium percentage", example = "0")
    val hePercent: Double,

    @field:Schema(description = "Maximum operating depth at ppO2 1.4, in meters", example = "33.8")
    val modM: Double,

    @field:Schema(description = "Gas used from this tank, in surface liters", example = "1680")
    val gasUsedL: Double,

    @field:Schema(description = "Surface consumption of this tank in bar/min. Null without average depth.", example = "1.1", nullable = true)
    val sacBarMin: Double?,
) {
    companion object {
        fun from(tank: DiveTank, durationMin: Double, avgDepthM: Double?, waterType: WaterType) = DiveTankDTO(
            id = tank.id!!,
            position = tank.position,
            volumeL = tank.volumeL,
            workingPressureBar = tank.workingPressureBar,
            material = tank.material,
            startPressureBar = tank.startPressureBar,
            endPressureBar = tank.endPressureBar,
            o2Percent = tank.o2Percent,
            hePercent = tank.hePercent,
            modM = DivePhysics.modM(tank.o2Percent, waterType = waterType).roundTo(1),
            gasUsedL = tank.gasUsedL().roundTo(0),
            sacBarMin = avgDepthM?.let {
                DivePhysics.sacBarMin(tank.pressureUsedBar().toDouble(), durationMin, it, waterType)?.roundTo(2)
            },
        )
    }
}

@Schema(description = "Gear used in a dive, as it was when the dive was logged")
data class DiveGearDTO(
    @field:Schema(description = "Id of this gear entry in the dive (use it as diveGearId to keep it on update)")
    val id: UUID,

    @field:Schema(description = "Id of the original gear. Null if the gear has since been deleted.", nullable = true)
    val gearId: UUID?,

    @field:Schema(description = "Gear category", example = "WETSUIT")
    val category: GearCategory,

    @field:Schema(description = "Gear name at the time of the dive", example = "Winter wetsuit")
    val name: String,

    @field:Schema(description = "Brand at the time of the dive", example = "Mares", nullable = true)
    val brand: String?,

    @field:Schema(description = "Model at the time of the dive", example = "Flexa 5-4-3", nullable = true)
    val model: String?,

    @field:Schema(description = "Category-specific attributes at the time of the dive", example = "{\"thicknessMm\": 5}")
    val attributes: Map<String, Any?>,
) {
    companion object {
        fun from(item: DiveGear) = DiveGearDTO(
            id = item.id!!,
            gearId = item.gear?.id,
            category = item.category,
            name = item.name,
            brand = item.brand,
            model = item.model,
            attributes = item.attributes,
        )
    }
}

@Schema(description = "Dive as shown in the logbook list")
data class DiveSummaryDTO(
    @field:Schema(description = "Dive id")
    val id: UUID,

    @field:Schema(description = "Progressive dive number, including dives logged before using the app", example = "42")
    val diveNumber: Long,

    @field:Schema(description = "Dive site")
    val site: DiveSiteSummaryDTO,

    @field:Schema(description = "Local date and time of entry at the dive site", example = "2026-08-10T09:30:00")
    val startLocal: LocalDateTime,

    @field:Schema(description = "IANA time zone of the dive site", example = "Europe/Rome")
    val timeZone: String,

    @field:Schema(description = "Total dive time in minutes", example = "52")
    val durationMinutes: Long,

    @field:Schema(description = "Maximum depth in meters", example = "24.5")
    val maxDepthM: Double,

    @field:Schema(description = "Surface consumption in L/min", example = "14.8", nullable = true)
    val rmvLMin: Double?,

    @field:Schema(description = "Personal rating from 1 to 5", example = "4", nullable = true)
    val rating: Int?,

    @field:Schema(description = "Dive characteristics", example = "[\"WALL\"]")
    val tags: Set<DiveTag>,
) {
    companion object {
        fun from(dive: Dive, diveNumber: Long) = DiveSummaryDTO(
            id = dive.id!!,
            diveNumber = diveNumber,
            site = DiveSiteSummaryDTO.from(dive.site),
            startLocal = dive.localStart().toLocalDateTime(),
            timeZone = dive.timeZone,
            durationMinutes = dive.duration().toMinutes(),
            maxDepthM = dive.maxDepthM,
            rmvLMin = dive.rmvLMin?.roundTo(1),
            rating = dive.rating,
            tags = dive.tags.toSet(),
        )
    }
}

@Schema(description = "Full dive details")
data class DiveDTO(
    @field:Schema(description = "Dive id")
    val id: UUID,

    @field:Schema(description = "Progressive dive number, including dives logged before using the app", example = "42")
    val diveNumber: Long,

    @field:Schema(description = "Dive site")
    val site: DiveSiteSummaryDTO,

    @field:Schema(description = "Entry time (absolute)", example = "2026-08-10T07:30:00Z")
    val startedAt: Instant,

    @field:Schema(description = "Exit time (absolute)", example = "2026-08-10T08:22:00Z")
    val endedAt: Instant,

    @field:Schema(description = "Local date and time of entry at the dive site", example = "2026-08-10T09:30:00")
    val startLocal: LocalDateTime,

    @field:Schema(description = "Local date and time of exit at the dive site", example = "2026-08-10T10:22:00")
    val endLocal: LocalDateTime,

    @field:Schema(description = "IANA time zone of the dive site", example = "Europe/Rome")
    val timeZone: String,

    @field:Schema(description = "Total dive time in minutes", example = "52")
    val durationMinutes: Long,

    @field:Schema(description = "Time since the end of the previous dive, in minutes. Null if this is the first dive.", example = "95", nullable = true)
    val surfaceIntervalMinutes: Long?,

    @field:Schema(description = "Main purpose", example = "RECREATIONAL", nullable = true)
    val purpose: DivePurpose?,

    @field:Schema(description = "Entry method", example = "BOAT", nullable = true)
    val entryMethod: EntryMethod?,

    @field:Schema(description = "Dive characteristics", example = "[\"WALL\", \"DRIFT\"]")
    val tags: Set<DiveTag>,

    @field:Schema(description = "Weather", example = "SUNNY", nullable = true)
    val weather: WeatherCondition?,

    @field:Schema(description = "Sea state", example = "CALM", nullable = true)
    val seaState: SeaState?,

    @field:Schema(description = "Current strength", example = "LIGHT", nullable = true)
    val currentStrength: CurrentStrength?,

    @field:Schema(description = "Visibility in meters", example = "15", nullable = true)
    val visibilityM: Double?,

    @field:Schema(description = "Air temperature in °C", example = "28", nullable = true)
    val airTemperatureC: Double?,

    @field:Schema(description = "Surface water temperature in °C", example = "24", nullable = true)
    val surfaceWaterTemperatureC: Double?,

    @field:Schema(description = "Bottom water temperature in °C", example = "18", nullable = true)
    val bottomWaterTemperatureC: Double?,

    @field:Schema(description = "Water type", example = "SALT")
    val waterType: WaterType,

    @field:Schema(description = "Maximum depth in meters", example = "24.5")
    val maxDepthM: Double,

    @field:Schema(description = "Average depth in meters", example = "14.2", nullable = true)
    val avgDepthM: Double?,

    @field:Schema(description = "Weight carried in kg", example = "6", nullable = true)
    val weightKg: Double?,

    @field:Schema(description = "Residual nitrogen factor/group at the start", example = "C", nullable = true)
    val residualNitrogenStart: String?,

    @field:Schema(description = "Residual nitrogen factor/group at the end", example = "F", nullable = true)
    val residualNitrogenEnd: String?,

    @field:Schema(description = "Residual nitrogen rate (TAR)", example = "1.2", nullable = true)
    val residualNitrogenRate: Double?,

    @field:Schema(description = "Profile phases in chronological order")
    val segments: List<DiveSegmentDTO>,

    @field:Schema(description = "Tanks used, with computed consumption")
    val tanks: List<DiveTankDTO>,

    @field:Schema(description = "Gear used, as it was when the dive was logged")
    val gear: List<DiveGearDTO>,

    @field:Schema(description = "Buddies of the dive, with confirmation status")
    val buddies: List<DiveBuddyDTO>,

    @field:Schema(description = "Total gas used from all tanks, in surface liters", example = "1680", nullable = true)
    val gasUsedL: Double?,

    @field:Schema(description = "Surface consumption in L/min (RMV). Null without tanks or average depth.", example = "14.8", nullable = true)
    val rmvLMin: Double?,

    @field:Schema(description = "Symptoms before the dive", example = "[]")
    val symptomsBefore: Set<DiveSymptom>,

    @field:Schema(description = "Symptoms after the dive", example = "[]")
    val symptomsAfter: Set<DiveSymptom>,

    @field:Schema(
        description = "True if symptoms after the dive are compatible with decompression sickness: " +
                "the app must show a warning recommending to contact DAN or emergency services. Not a diagnosis.",
        example = "false",
    )
    val dcsWarning: Boolean,

    @field:Schema(description = "Free notes about symptoms", nullable = true)
    val symptomsNotes: String?,

    @field:Schema(description = "Personal rating from 1 to 5", example = "4", nullable = true)
    val rating: Int?,

    @field:Schema(description = "Free notes", nullable = true)
    val notes: String?,

    @field:Schema(description = "Creation timestamp")
    val createdAt: Instant,

    @field:Schema(description = "Last update timestamp")
    val updatedAt: Instant,
) {
    companion object {
        fun from(dive: Dive, diveNumber: Long, surfaceIntervalMinutes: Long?): DiveDTO {
            val durationMin = dive.duration().seconds / 60.0

            return DiveDTO(
                id = dive.id!!,
                diveNumber = diveNumber,
                site = DiveSiteSummaryDTO.from(dive.site),
                startedAt = dive.startedAt,
                endedAt = dive.endedAt,
                startLocal = dive.localStart().toLocalDateTime(),
                endLocal = dive.localEnd().toLocalDateTime(),
                timeZone = dive.timeZone,
                durationMinutes = dive.duration().toMinutes(),
                surfaceIntervalMinutes = surfaceIntervalMinutes,
                purpose = dive.purpose,
                entryMethod = dive.entryMethod,
                tags = dive.tags.toSet(),
                weather = dive.weather,
                seaState = dive.seaState,
                currentStrength = dive.currentStrength,
                visibilityM = dive.visibilityM,
                airTemperatureC = dive.airTemperatureC,
                surfaceWaterTemperatureC = dive.surfaceWaterTemperatureC,
                bottomWaterTemperatureC = dive.bottomWaterTemperatureC,
                waterType = dive.waterType,
                maxDepthM = dive.maxDepthM,
                avgDepthM = dive.avgDepthM?.roundTo(1),
                weightKg = dive.weightKg,
                residualNitrogenStart = dive.residualNitrogenStart,
                residualNitrogenEnd = dive.residualNitrogenEnd,
                residualNitrogenRate = dive.residualNitrogenRate,
                segments = dive.segments.map(DiveSegmentDTO::from),
                tanks = dive.tanks.map { DiveTankDTO.from(it, durationMin, dive.avgDepthM, dive.waterType) },
                gear = dive.gear.map(DiveGearDTO::from),
                buddies = dive.buddies.map(DiveBuddyDTO::from),
                gasUsedL = dive.gasUsedL?.roundTo(0),
                rmvLMin = dive.rmvLMin?.roundTo(1),
                symptomsBefore = dive.symptomsBefore.toSet(),
                symptomsAfter = dive.symptomsAfter.toSet(),
                dcsWarning = dive.hasDcsWarningSymptoms(),
                symptomsNotes = dive.symptomsNotes,
                rating = dive.rating,
                notes = dive.notes,
                createdAt = dive.createdAt,
                updatedAt = dive.updatedAt,
            )
        }
    }
}