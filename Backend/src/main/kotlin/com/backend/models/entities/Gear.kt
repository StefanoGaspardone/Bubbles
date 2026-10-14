package com.backend.models.entities

import com.backend.models.enums.GearCategory
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "gears")
class Gear(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    var owner: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    var category: GearCategory,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "brand", nullable = true, length = 100)
    var brand: String? = null,

    @Column(name = "model", nullable = true, length = 100)
    var model: String? = null,

    @Column(name = "serial_number", nullable = true, length = 100)
    var serialNumber: String? = null,

    @Column(name = "purchase_date", nullable = true)
    var purchaseDate: LocalDate? = null,

    @Column(name = "last_service_date", nullable = true)
    var lastServiceDate: LocalDate? = null,

    @Column(name = "service_interval_months", nullable = true)
    var serviceIntervalMonths: Int? = null,

    @Column(name = "notes", nullable = true, length = 1000)
    var notes: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    var attributes: Map<String, Any?> = emptyMap(),

    @Column(name = "archived_at", nullable = true)
    var archivedAt: Instant? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {

    fun isArchived(): Boolean =
        archivedAt != null

    fun archive(now: Instant = Instant.now()) {
        if(archivedAt == null) archivedAt = now
    }

    fun restore() {
        archivedAt = null
    }

    fun nextServiceDate(): LocalDate? {
        val interval = serviceIntervalMonths ?: return null
        val base = lastServiceDate ?: purchaseDate ?: return null
        return base.plusMonths(interval.toLong())
    }

    fun isServiceDue(today: LocalDate = LocalDate.now()): Boolean =
        nextServiceDate()?.let { !today.isBefore(it) } ?: false

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as Gear
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "Gear(id=$id, category=$category, name=$name, archivedAt=$archivedAt)"
}