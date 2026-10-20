package com.backend.models.enums

enum class FileFormat(val mimeType: String, val extension: String) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    PDF("application/pdf", "pdf"),
}

enum class FileVisibility(val keyPrefix: String) {
    PUBLIC("public"),
    PRIVATE("private"),
}

private val IMAGES = setOf(FileFormat.JPEG, FileFormat.PNG, FileFormat.WEBP)
private val IMAGES_AND_PDF = IMAGES + FileFormat.PDF

private const val MB = 1024L * 1024L

enum class FileOwnerType(
    val visibility: FileVisibility,
    val allowedFormats: Set<FileFormat>,
    val maxSizeBytes: Long,
) {
    USER_AVATAR(FileVisibility.PUBLIC, IMAGES, 5 * MB),
    DIVE_PHOTO(FileVisibility.PUBLIC, IMAGES, 15 * MB),
    CERTIFICATION(FileVisibility.PRIVATE, IMAGES_AND_PDF, 15 * MB),
    GEAR(FileVisibility.PRIVATE, IMAGES_AND_PDF, 15 * MB);

    val keySegment: String = name.lowercase().replace('_', '-')
}