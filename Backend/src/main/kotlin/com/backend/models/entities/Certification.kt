package com.backend.models.entities

import com.backend.models.enums.CertificationAgency
import com.backend.models.enums.CertificationType
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "certifications")
class Certification(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    var owner: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "agency", nullable = false, length = 20)
    var agency: CertificationAgency,

    @Column(name = "agency_name", nullable = true, length = 100)
    var agencyName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "certification_type", nullable = false, length = 20)
    var certificationType: CertificationType,

    @Column(name = "course_name", nullable = false, length = 150)
    var courseName: String,

    @Column(name = "certification_number", nullable = true, length = 100)
    var certificationNumber: String? = null,

    @Column(name = "issue_date", nullable = false)
    var issueDate: LocalDate,

    @Column(name = "expiry_date", nullable = true)
    var expiryDate: LocalDate? = null,

    @Column(name = "instructor_name", nullable = true, length = 150)
    var instructorName: String? = null,

    @Column(name = "instructor_number", nullable = true, length = 50)
    var instructorNumber: String? = null,

    @Column(name = "dive_center", nullable = true, length = 150)
    var diveCenter: String? = null,

    @Column(name = "notes", nullable = true, length = 1000)
    var notes: String? = null,

    @Column(name = "front_image_id", nullable = true)
    var frontImageId: UUID? = null,

    @Column(name = "back_image_id", nullable = true)
    var backImageId: UUID? = null,

    @Column(name = "document_id", nullable = true)
    var documentId: UUID? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {

    fun displayAgency(): String =
        agencyName ?: agency.name

    fun isExpired(today: LocalDate = LocalDate.now()): Boolean =
        expiryDate?.isBefore(today) ?: false

    fun isExpiringWithin(days: Long, today: LocalDate = LocalDate.now()): Boolean {
        val expiry = expiryDate ?: return false
        return !isExpired(today) && ChronoUnit.DAYS.between(today, expiry) <= days
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as Certification
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "Certification(id=$id, agency=$agency, courseName=$courseName)"
}