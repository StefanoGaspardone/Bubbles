package com.backend.utils

import com.backend.models.enums.WaterType
import kotlin.math.pow
import kotlin.math.roundToLong

object DivePhysics {

    const val SURFACE_PRESSURE_BAR = 1.0
    const val DEFAULT_MAX_PPO2_BAR = 1.4

    private const val SALT_WATER_METERS_PER_BAR = 10.0
    private const val FRESH_WATER_METERS_PER_BAR = 10.3

    fun metersPerBar(waterType: WaterType): Double = when(waterType) {
        WaterType.SALT -> SALT_WATER_METERS_PER_BAR
        WaterType.FRESH -> FRESH_WATER_METERS_PER_BAR
    }

    /** Pressione assoluta (bar) a una data profondità. */
    fun ambientPressureBar(depthM: Double, waterType: WaterType): Double =
        SURFACE_PRESSURE_BAR + depthM / metersPerBar(waterType)

    fun sacBarMin(pressureUsedBar: Double, durationMin: Double, avgDepthM: Double, waterType: WaterType): Double? {
        if(durationMin <= 0.0) return null
        return pressureUsedBar / durationMin / ambientPressureBar(avgDepthM, waterType)
    }

    fun rmvLMin(gasUsedL: Double, durationMin: Double, avgDepthM: Double, waterType: WaterType): Double? {
        if(durationMin <= 0.0) return null
        return gasUsedL / durationMin / ambientPressureBar(avgDepthM, waterType)
    }

    fun modM(o2Percent: Double, maxPpO2Bar: Double = DEFAULT_MAX_PPO2_BAR, waterType: WaterType = WaterType.SALT): Double {
        require(o2Percent > 0.0) { "O2 percentage must be positive" }
        return (maxPpO2Bar / (o2Percent / 100.0) - SURFACE_PRESSURE_BAR) * metersPerBar(waterType)
    }

    fun ppO2Bar(o2Percent: Double, depthM: Double, waterType: WaterType = WaterType.SALT): Double =
        (o2Percent / 100.0) * ambientPressureBar(depthM, waterType)
}

fun Double.roundTo(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return (this * factor).roundToLong() / factor
}