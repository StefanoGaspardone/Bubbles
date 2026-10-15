package com.backend.models.entities

import com.backend.models.enums.CurrentStrength
import com.backend.models.enums.DivePurpose
import com.backend.models.enums.DiveSymptom
import com.backend.models.enums.DiveTag
import com.backend.models.enums.EntryMethod
import com.backend.models.enums.SeaState
import com.backend.models.enums.WaterType
import com.backend.models.enums.WeatherCondition
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "dives")
class Dive(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    var owner: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    var site: DiveSite,

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant,

    @Column(name = "ended_at", nullable = false)
    var endedAt: Instant,

    @Column(name = "time_zone", nullable = false, length = 64)
    var timeZone: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = true, length = 20)
    var purpose: DivePurpose? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_method", nullable = true, length = 20)
    var entryMethod: EntryMethod? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "weather", nullable = true, length = 20)
    var weather: WeatherCondition? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "sea_state", nullable = true, length = 20)
    var seaState: SeaState? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "current_strength", nullable = true, length = 20)
    var currentStrength: CurrentStrength? = null,

    @Column(name = "visibility_m", nullable = true)
    var visibilityM: Double? = null,

    @Column(name = "air_temperature_c", nullable = true)
    var airTemperatureC: Double? = null,

    @Column(name = "surface_water_temperature_c", nullable = true)
    var surfaceWaterTemperatureC: Double? = null,

    @Column(name = "bottom_water_temperature_c", nullable = true)
    var bottomWaterTemperatureC: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "water_type", nullable = false, length = 10)
    var waterType: WaterType = WaterType.SALT,

    @Column(name = "max_depth_m", nullable = false)
    var maxDepthM: Double,

    @Column(name = "avg_depth_m", nullable = true)
    var avgDepthM: Double? = null,

    @Column(name = "weight_kg", nullable = true)
    var weightKg: Double? = null,

    @Column(name = "residual_nitrogen_start", nullable = true, length = 10)
    var residualNitrogenStart: String? = null,

    @Column(name = "residual_nitrogen_end", nullable = true, length = 10)
    var residualNitrogenEnd: String? = null,

    @Column(name = "residual_nitrogen_rate", nullable = true)
    var residualNitrogenRate: Double? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "segments", nullable = false, columnDefinition = "jsonb")
    var segments: List<DiveSegment> = emptyList(),

    @Column(name = "gas_used_l", nullable = true)
    var gasUsedL: Double? = null,

    @Column(name = "rmv_l_min", nullable = true)
    var rmvLMin: Double? = null,

    @Column(name = "symptoms_notes", nullable = true, length = 1000)
    var symptomsNotes: String? = null,

    @Column(name = "rating", nullable = true)
    var rating: Int? = null,

    @Column(name = "notes", nullable = true, length = 4000)
    var notes: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "dive_tags", joinColumns = [JoinColumn(name = "dive_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 20)
    var tags: MutableSet<DiveTag> = mutableSetOf()

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "dive_symptoms_before", joinColumns = [JoinColumn(name = "dive_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "symptom", nullable = false, length = 30)
    var symptomsBefore: MutableSet<DiveSymptom> = mutableSetOf()

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "dive_symptoms_after", joinColumns = [JoinColumn(name = "dive_id")])
    @Enumerated(EnumType.STRING)
    @Column(name = "symptom", nullable = false, length = 30)
    var symptomsAfter: MutableSet<DiveSymptom> = mutableSetOf()

    @OneToMany(mappedBy = "dive", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("position ASC")
    val tanks: MutableList<DiveTank> = mutableListOf()

    @OneToMany(mappedBy = "dive", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("position ASC")
    val gear: MutableList<DiveGear> = mutableListOf()

    @OneToMany(mappedBy = "dive", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("position ASC")
    val buddies: MutableList<DiveBuddy> = mutableListOf()

    fun replaceBuddies(newBuddies: List<DiveBuddy>) {
        require(newBuddies.all { it.dive === this }) { "All buddies must belong to this dive" }

        buddies.clear()
        newBuddies.forEachIndexed { index, buddy ->
            buddy.position = index
            buddies.add(buddy)
        }
    }

    fun duration(): Duration =
        Duration.between(startedAt, endedAt)

    fun localStart(): ZonedDateTime =
        startedAt.atZone(ZoneId.of(timeZone))

    fun localEnd(): ZonedDateTime =
        endedAt.atZone(ZoneId.of(timeZone))

    fun hasDcsWarningSymptoms(): Boolean =
        symptomsAfter.any { it in DiveSymptom.DCS_WARNING_SYMPTOMS }

    fun replaceTanks(newTanks: List<DiveTank>) {
        require(newTanks.all { it.dive === this }) { "All tanks must belong to this dive" }

        tanks.clear()
        newTanks.forEachIndexed { index, tank ->
            tank.position = index
            tanks.add(tank)
        }
    }

    fun replaceGear(newGear: List<DiveGear>) {
        require(newGear.all { it.dive === this }) { "All gear must belong to this dive" }

        gear.clear()
        newGear.forEachIndexed { index, item ->
            item.position = index
            gear.add(item)
        }
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as Dive
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "Dive(id=$id, startedAt=$startedAt, maxDepthM=$maxDepthM)"
}