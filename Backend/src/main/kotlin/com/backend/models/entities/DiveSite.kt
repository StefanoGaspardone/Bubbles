package com.backend.models.entities

import com.backend.models.enums.DiveSiteSource
import com.backend.models.enums.DiveSiteType
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "dive_sites")
class DiveSite(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true, updatable = false)
    var owner: User? = null,

    @Column(name = "name", nullable = false, length = 150)
    var name: String,

    @Column(name = "locality", nullable = true, length = 150)
    var locality: String? = null,

    @Column(name = "country_code", nullable = true, length = 2)
    var countryCode: String? = null,

    @Column(name = "latitude", nullable = false)
    var latitude: Double,

    @Column(name = "longitude", nullable = false)
    var longitude: Double,

    @Enumerated(EnumType.STRING)
    @Column(name = "site_type", nullable = true, length = 20)
    var siteType: DiveSiteType? = null,

    @Column(name = "min_depth_m", nullable = true)
    var minDepthM: Double? = null,

    @Column(name = "max_depth_m", nullable = true)
    var maxDepthM: Double? = null,

    @Column(name = "description", nullable = true, length = 2000)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20, updatable = false)
    var source: DiveSiteSource = DiveSiteSource.USER,

    @Column(name = "external_id", nullable = true, length = 100, updatable = false)
    var externalId: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {

    fun isPublic(): Boolean =
        owner == null

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as DiveSite
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "DiveSite(id=$id, name=$name, source=$source)"
}