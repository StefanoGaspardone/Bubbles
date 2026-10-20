package com.backend.models.dtos

import com.backend.models.entities.UploadedFile
import com.backend.models.enums.FileVisibility
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.core.io.Resource
import java.time.Instant
import java.util.UUID

@Schema(description = "Metadata of a stored file")
data class UploadedFileDTO(
    @field:Schema(description = "File id", example = "4e2f1a9c-7b3d-4c5e-9f8a-1b2c3d4e5f60")
    val id: UUID,

    @field:Schema(description = "Original file name, for display and download", example = "padi-aowd-front.jpg")
    val fileName: String,

    @field:Schema(description = "Content type detected from the file content", example = "image/jpeg")
    val contentType: String,

    @field:Schema(description = "Size in bytes", example = "482133")
    val size: Long,

    @field:Schema(description = "PUBLIC files can be used directly in <img src>; PRIVATE files require the access token", example = "PRIVATE")
    val visibility: FileVisibility,

    @field:Schema(
        description = "Where to get the content. PUBLIC: absolute object storage URL, no authentication. PRIVATE: API path to call with the access token.",
        example = "/api/v1/files/4e2f1a9c-7b3d-4c5e-9f8a-1b2c3d4e5f60/content",
    )
    val url: String,

    @field:Schema(description = "Upload timestamp")
    val createdAt: Instant,
) {
    companion object {
        fun from(file: UploadedFile, url: String) = UploadedFileDTO(
            id = file.id!!,
            fileName = file.fileName,
            contentType = file.contentType,
            size = file.size,
            visibility = file.visibility,
            url = url,
            createdAt = file.createdAt,
        )
    }
}

data class StoredFileContent(
    val resource: Resource,
    val fileName: String,
    val contentType: String,
    val size: Long,
)