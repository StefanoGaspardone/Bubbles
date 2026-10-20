package com.backend.controllers

import com.backend.configs.OpenAPIConfig
import com.backend.exceptions.ErrorResponse
import com.backend.models.dtos.CertificationDTO
import com.backend.models.enums.CertificationAttachmentSlot
import com.backend.security.CurrentUser
import com.backend.services.CertificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Tag(name = "Certifications", description = "REST APIs to manage the current user's certifications, cards and diving documents")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/certifications/{certificationId}/attachments/{slot}")
@PreAuthorize("hasRole('USER')")
class CertificationAttachmentController(
    private val certificationService: CertificationService,
) {

    @Operation(
        summary = "Set certification attachment",
        description = "Upload the card front photo (FRONT), back photo (BACK) or official PDF (DOCUMENT) of one of the " +
                "current user's certifications, replacing the previous file in that slot. " +
                "FRONT and BACK accept JPG, PNG, WEBP; DOCUMENT accepts PDF. Up to 15 MB. " +
                "Attachments are private: read them with the access token from /api/v1/files/{id}/content."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Attachment set, updated certification returned",
                content = [Content(schema = Schema(implementation = CertificationDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Missing or empty file, or invalid slot",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "MissingFileExample",
                        summary = "Missing file example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"file: is required\"}"
                    ), ExampleObject(
                        name = "InvalidSlotExample",
                        summary = "Invalid slot example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"slot: invalid value 'SIDE'\"}"
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
                responseCode = "404", description = "Not Found - Certification does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "CertificationNotFoundExample",
                        summary = "Certification not found example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Certification not found\"}"
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
                responseCode = "415", description = "Unsupported Media Type - Format not allowed for the slot",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "UnsupportedTypeExample",
                        summary = "Unsupported file type example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":415,\"error\":\"Unsupported Media Type\",\"message\":\"Unsupported file type: allowed formats are PDF\"}"
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
    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun setCertificationAttachment(
        @Parameter(description = "Certification id", example = "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f")
        @PathVariable certificationId: UUID,
        @Parameter(description = "Attachment slot", example = "FRONT")
        @PathVariable slot: CertificationAttachmentSlot,
        @Parameter(description = "Image (FRONT, BACK) or PDF (DOCUMENT), max 15 MB")
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<CertificationDTO> =
        ResponseEntity.ok(certificationService.setAttachment(CurrentUser.id(), certificationId, slot, file))

    @Operation(
        summary = "Remove certification attachment",
        description = "Remove the file in the given slot of one of the current user's certifications. " +
                "Idempotent: returns 204 even if the slot is empty."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Attachment removed",
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
                responseCode = "404", description = "Not Found - Certification does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "CertificationNotFoundExample",
                        summary = "Certification not found example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Certification not found\"}"
                    )]
                )]
            ),
        ]
    )
    @DeleteMapping
    fun removeCertificationAttachment(
        @Parameter(description = "Certification id", example = "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f")
        @PathVariable certificationId: UUID,
        @Parameter(description = "Attachment slot", example = "FRONT")
        @PathVariable slot: CertificationAttachmentSlot,
    ): ResponseEntity<Unit> {
        certificationService.removeAttachment(CurrentUser.id(), certificationId, slot)
        return ResponseEntity.noContent().build()
    }
}