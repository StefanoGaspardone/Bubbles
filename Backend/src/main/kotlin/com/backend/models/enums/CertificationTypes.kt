package com.backend.models.enums

enum class CertificationAgency {
    PADI,
    SSI,
    CMAS,
    NAUI,
    SDI,
    TDI,
    RAID,
    GUE,
    BSAC,
    IANTD,
    DAN,
    FIPSAS,
    OTHER,
}

enum class CertificationType {
    RECREATIONAL,
    SPECIALTY,
    TECHNICAL,
    PROFESSIONAL,
    FIRST_AID,
    INSURANCE,
    OTHER,
}

enum class CertificationAttachmentSlot(val allowedFormats: Set<FileFormat>) {
    FRONT(setOf(FileFormat.JPEG, FileFormat.PNG, FileFormat.WEBP)),
    BACK(setOf(FileFormat.JPEG, FileFormat.PNG, FileFormat.WEBP)),
    DOCUMENT(setOf(FileFormat.PDF)),
}