package com.backend.models.enums

enum class DivePurpose {
    RECREATIONAL,
    TRAINING,
    EXPLORATION,
    PHOTOGRAPHY,
    SCIENTIFIC,
    WORK,
}

enum class EntryMethod {
    SHORE,
    BOAT,
}

enum class DiveTag {
    NIGHT,
    DRIFT,
    DEEP,
    WRECK,
    WALL,
    SHOAL,
    CAVE,
    ICE,
    ALTITUDE,
    DECOMPRESSION,
}

enum class WeatherCondition {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    STORM,
}

enum class SeaState {
    CALM,
    SLIGHT,
    MODERATE,
    ROUGH,
}

enum class CurrentStrength {
    NONE,
    LIGHT,
    MODERATE,
    STRONG,
}

enum class WaterType {
    SALT,
    FRESH,
}

enum class DiveSymptom {
    FATIGUE,
    HEADACHE,
    NAUSEA,
    EAR_PAIN,
    SINUS_PAIN,
    JOINT_PAIN,
    SKIN_RASH_ITCHING,
    TINGLING_NUMBNESS,
    DIZZINESS,
    SHORTNESS_OF_BREATH,
    COLD;

    companion object {
        val DCS_WARNING_SYMPTOMS: Set<DiveSymptom> = setOf(
            JOINT_PAIN,
            SKIN_RASH_ITCHING,
            TINGLING_NUMBNESS,
            DIZZINESS,
            SHORTNESS_OF_BREATH,
        )
    }
}

enum class DiveSegmentType {
    DESCENT,
    BOTTOM,
    DEEP_STOP,
    ASCENT,
    DECO_STOP,
    SAFETY_STOP,
    SURFACE,
}

enum class TankMaterial {
    STEEL,
    ALUMINIUM,
}

enum class DiveSiteType {
    REEF,
    WALL,
    WRECK,
    SHOAL,
    CAVE,
    LAKE,
    QUARRY,
    OTHER,
}

enum class DiveSiteSource {
    USER,
    OPENSTREETMAP,
}