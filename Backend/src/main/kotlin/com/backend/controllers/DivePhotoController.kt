package com.backend.controllers

import com.backend.configs.OpenAPIConfig
import com.backend.exceptions.ErrorResponse
import com.backend.models.dtos.UploadedFileDTO
import com.backend.security.CurrentUser
import com.backend.services.DiveService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.net.URI
import java.util.UUID

@Tag(name = "Dives", description = "REST APIs to manage the current user's logbook: list, create, update and delete dives")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/dives/{diveId}/photos")
@PreAuthorize("hasRole('USER')")
class DivePhotoController(
    private val diveService: DiveService,
) {

    @Operation(
        summary = "List dive photos",
        description = "List the photos of one of the current user's dives, most recent first. " +
                "Photos are public: use their url directly in <img src>."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - List of photos (empty if none)",
                content = [Content(array = ArraySchema(schema = Schema(implementation = UploadedFileDTO::class)))]
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
                responseCode = "404", description = "Not Found - Dive does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "DiveNotFoundExample",
                        summary = "Dive not found example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Dive not found\"}"
                    )]
                )]
            ),
        ]
    )
    @GetMapping
    fun listDivePhotos(
        @Parameter(description = "Dive id", example = "5d7e2a9c-3b4f-4e1a-8c6d-2f1e0a9b8c7d")
        @PathVariable diveId: UUID,
    ): ResponseEntity<List<UploadedFileDTO>> =
        ResponseEntity.ok(diveService.listPhotos(CurrentUser.id(), diveId))

    @Operation(
        summary = "Add dive photo",
        description = "Upload a photo to one of the current user's dives. Allowed formats: JPG, PNG, WEBP, up to 15 MB, " +
                "at most 20 photos per dive. The format is detected from the file content."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201", description = "Created - Photo added",
                headers = [Header(name = "Location", description = "URL of the photo content", schema = Schema(type = "string"))],
                content = [Content(schema = Schema(implementation = UploadedFileDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Missing or empty file",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "MissingFileExample",
                        summary = "Missing file example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"file: is required\"}"
                    )]
                )]
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
                responseCode = "404", description = "Not Found - Dive does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "DiveNotFoundExample",
                        summary = "Dive not found example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Dive not found\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "409", description = "Conflict - Maximum number of photos reached",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "TooManyFilesExample",
                        summary = "Too many photos example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":409,\"error\":\"Conflict\",\"message\":\"Maximum number of files reached (20)\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "413", description = "Payload Too Large - File larger than 15 MB",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "TooLargeExample",
                        summary = "File too large example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":413,\"error\":\"Payload Too Large\",\"message\":\"File is too large: maximum size is 15 MB\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "415", description = "Unsupported Media Type - Not a JPG, PNG or WEBP image",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "UnsupportedTypeExample",
                        summary = "Unsupported file type example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":415,\"error\":\"Unsupported Media Type\",\"message\":\"Unsupported file type: allowed formats are JPG, PNG, WEBP\"}"
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
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"Unable to store the file, please try again later\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun addDivePhoto(
        @Parameter(description = "Dive id", example = "5d7e2a9c-3b4f-4e1a-8c6d-2f1e0a9b8c7d")
        @PathVariable diveId: UUID,
        @Parameter(description = "Photo (JPG, PNG or WEBP, max 15 MB)")
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<UploadedFileDTO> {
        val created = diveService.addPhoto(CurrentUser.id(), diveId, file)
        return ResponseEntity.created(URI.create(created.url)).body(created)
    }

    @Operation(summary = "Delete dive photo", description = "Delete a photo from one of the current user's dives.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Photo deleted",
                content = [Content(schema = Schema(hidden = true))]
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
                responseCode = "404", description = "Not Found - Dive or photo not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "DiveNotFoundExample",
                        summary = "Dive not found example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Dive not found\"}"
                    ), ExampleObject(
                        name = "FileNotFoundExample",
                        summary = "Photo not found example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"File not found\"}"
                    )]
                )]
            ),
        ]
    )
    @DeleteMapping("/{fileId}")
    fun deleteDivePhoto(
        @Parameter(description = "Dive id", example = "5d7e2a9c-3b4f-4e1a-8c6d-2f1e0a9b8c7d")
        @PathVariable diveId: UUID,
        @Parameter(description = "Photo file id", example = "4e2f1a9c-7b3d-4c5e-9f8a-1b2c3d4e5f60")
        @PathVariable fileId: UUID,
    ): ResponseEntity<Unit> {
        diveService.deletePhoto(CurrentUser.id(), diveId, fileId)
        return ResponseEntity.noContent().build()
    }
}