package com.backend.models.entities

import com.backend.models.enums.TankMaterial
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.util.UUID

@Entity
@Table(name = "dive_tanks")
class DiveTank(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dive_id", nullable = false, updatable = false)
    var dive: Dive,

    @Column(name = "position", nullable = false)
    var position: Int = 0,

    @Column(name = "volume_l", nullable = false)
    var volumeL: Double,

    @Column(name = "working_pressure_bar", nullable = true)
    var workingPressureBar: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "material", nullable = true, length = 20)
    var material: TankMaterial? = null,

    @Column(name = "start_pressure_bar", nullable = false)
    var startPressureBar: Int,

    @Column(name = "end_pressure_bar", nullable = false)
    var endPressureBar: Int,

    @Column(name = "o2_percent", nullable = false)
    var o2Percent: Double = 21.0,

    @Column(name = "he_percent", nullable = false)
    var hePercent: Double = 0.0,
) {

    fun isAir(): Boolean =
        o2Percent == 21.0 && hePercent == 0.0

    fun pressureUsedBar(): Int =
        startPressureBar - endPressureBar

    fun gasUsedL(): Double =
        pressureUsedBar() * volumeL

    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

        other as DiveTank
        return id != null && id == other.id
    }

    override fun hashCode(): Int =
        Hibernate.getClass(this).hashCode()

    override fun toString(): String =
        "DiveTank(id=$id, position=$position, volumeL=$volumeL, start=$startPressureBar, end=$endPressureBar, o2=$o2Percent)"
}