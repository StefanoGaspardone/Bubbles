package com.backend.models.entities

import com.backend.models.enums.GearCategory
import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "dive_gear")
class DiveGear(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dive_id", nullable = false, updatable = false)
    var dive: Dive,

    @Column(name = "position", nullable = false)
    var position: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gear_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var gear: Gear? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    var category: GearCategory,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "brand", nullable = true, length = 100)
    var brand: String? = null,

    @Column(name = "model", nullable = true, length = 100)
    var model: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    var attributes: Map<String, Any?> = emptyMap(),
) {

    companion object {
        fun snapshotOf(source: Gear, dive: Dive): DiveGear = DiveGear(
            dive = dive,
            gear = source,
            category = source.category,
            name = source.name,
            brand = source.brand,
            model = source.model,
            attributes = source.attributes,
        )
    }

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as DiveGear
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "DiveGear(id=$id, category=$category, name=$name)"
}