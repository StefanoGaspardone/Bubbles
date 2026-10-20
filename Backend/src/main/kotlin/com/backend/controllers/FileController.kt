package com.backend.controllers

import com.backend.configs.OpenAPIConfig
import com.backend.exceptions.ErrorResponse
import com.backend.models.dtos.UploadedFileDTO
import com.backend.security.CurrentUser
import com.backend.services.FileDelivery
import com.backend.services.UploadedFileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID

@Tag(name = "Files", description = "REST APIs to read stored files: metadata and content")
@RestController
@RequestMapping("/api/v1/files")
class FileController(
    private val uploadedFileService: UploadedFileService,
) {

    private companion object {
        val PUBLIC_REDIRECT_CACHE: Duration = Duration.ofDays(1)
    }

    @Operation(
        summary = "Get file metadata",
        description = "Get the metadata of a file: public files are visible to every authenticated user, private files only to their owner."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - File metadata",
                content = [Content(schema = Schema(implementation = UploadedFileDTO::class))]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized - Missing, invalid or expired access token",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "TokenExpiredExample",
                        summary = "Expired token example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - File does not exist, or is private and belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "FileNotFoundExample",
                        summary = "File not found example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"File not found\"}"
                    )]
                )]
            ),
        ]
    )
    @SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{fileId}")
    fun getFileMetadata(
        @Parameter(description = "File id", example = "4e2f1a9c-7b3d-4c5e-9f8a-1b2c3d4e5f60")
        @PathVariable fileId: UUID,
    ): ResponseEntity<UploadedFileDTO> =
        ResponseEntity.ok(uploadedFileService.getFile(CurrentUser.id(), fileId))

    @Operation(
        summary = "Get file content",
        description = "Get the content of a file. The access token is optional.\n\n" +
                "- **Public files** (avatars, dive photos): 302 redirect to the object storage URL, also without token. " +
                "Can be used directly in `<img src>`.\n" +
                "- **Private files** (certifications, gear documents): streamed only to their owner, with the access token. " +
                "In the browser, fetch it with the token and display it as a blob.\n\n" +
                "A private file requested without token or by another user is reported as not found."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Private file content",
                headers = [
                    Header(name = "Content-Disposition", description = "inline, or attachment when download=true", schema = Schema(type = "string")),
                    Header(name = "Content-Length", description = "File size in bytes", schema = Schema(type = "integer")),
                ],
                content = [Content(mediaType = "application/octet-stream", schema = Schema(type = "string", format = "binary"))]
            ),
            ApiResponse(
                responseCode = "302", description = "Found - Public file: redirect to the object storage URL",
                headers = [Header(name = "Location", description = "Public object storage URL", schema = Schema(type = "string"))],
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - File does not exist, or is private and not accessible",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "FileNotFoundExample",
                        summary = "File not found example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"File not found\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "503", description = "Service Unavailable - Object storage temporarily unreachable",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "StorageUnavailableExample",
                        summary = "Storage unavailable example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"File temporarily unavailable, please try again later\"}"
                    )]
                )]
            ),
        ]
    )
    @SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
    @GetMapping("/{fileId}/content")
    fun getFileContent(
        @Parameter(description = "File id", example = "4e2f1a9c-7b3d-4c5e-9f8a-1b2c3d4e5f60")
        @PathVariable fileId: UUID,
        @Parameter(description = "Private files only: true to download as attachment instead of displaying inline", example = "false")
        @RequestParam(defaultValue = "false") download: Boolean,
    ): ResponseEntity<Resource> =
        when(val delivery = uploadedFileService.deliverFile(CurrentUser.idOrNull(), fileId)) {
            is FileDelivery.Redirect -> ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(delivery.url))
                .cacheControl(CacheControl.maxAge(PUBLIC_REDIRECT_CACHE).cachePublic())
                .build()

            is FileDelivery.Stream -> {
                val content = delivery.content
                val disposition = (if(download) ContentDisposition.attachment() else ContentDisposition.inline())
                    .filename(content.fileName, StandardCharsets.UTF_8)
                    .build()

                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(content.contentType))
                    .contentLength(content.size)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    // Documenti personali: nessuna cache condivisa né su disco
                    .cacheControl(CacheControl.noStore().cachePrivate())
                    // Il browser non deve reinterpretare il tipo dichiarato
                    .header("X-Content-Type-Options", "nosniff")
                    .body(content.resource)
            }
        }
}