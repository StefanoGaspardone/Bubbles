package com.backend.controllers

import com.backend.configs.OpenAPIConfig
import com.backend.exceptions.ErrorResponse
import com.backend.models.dtos.CertificationDTO
import com.backend.models.dtos.CertificationRequestDTO
import com.backend.models.enums.CertificationAgency
import com.backend.models.enums.CertificationType
import com.backend.security.CurrentUser
import com.backend.services.CertificationService
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
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@Tag(name = "Certifications", description = "REST APIs to manage the current user's certifications, cards and diving documents")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/certifications")
@PreAuthorize("hasRole('USER')")
class CertificationController(
    private val certificationService: CertificationService,
) {

    @Operation(
        summary = "List my certifications",
        description = "List the current user's certifications, cards and documents, most recent first, with optional filters. Each item reports whether it has expired or expires soon."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - List of certifications (empty if none)",
                content = [Content(array = ArraySchema(schema = Schema(implementation = CertificationDTO::class)))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid query parameter",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidParameterExample",
                        summary = "Invalid agency example",
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"agency: invalid value 'XYZ'\"}"
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
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
        ]
    )
    @GetMapping
    fun listMyCertifications(
        @Parameter(description = "Filter by training agency", example = "PADI")
        @RequestParam(required = false) agency: CertificationAgency?,
        @Parameter(description = "Filter by document type", example = "RECREATIONAL")
        @RequestParam(required = false) certificationType: CertificationType?,
        @Parameter(description = "Whether to include expired documents", example = "true")
        @RequestParam(defaultValue = "true") includeExpired: Boolean,
    ): ResponseEntity<List<CertificationDTO>> =
        ResponseEntity.ok(certificationService.listCertifications(CurrentUser.id(), agency, certificationType, includeExpired))

    @Operation(summary = "Get certification", description = "Get one of the current user's certifications.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Certification details",
                content = [Content(schema = Schema(implementation = CertificationDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid certification id",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidIdExample",
                        summary = "Invalid id example",
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"certificationId: invalid value 'abc'\"}"
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
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
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
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Certification not found\"}"
                    )]
                )]
            ),
        ]
    )
    @GetMapping("/{certificationId}")
    fun getMyCertification(
        @Parameter(description = "Certification id", example = "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f")
        @PathVariable certificationId: UUID,
    ): ResponseEntity<CertificationDTO> =
        ResponseEntity.ok(certificationService.getCertification(CurrentUser.id(), certificationId))

    @Operation(
        summary = "Add certification",
        description = "Add a certification, card or diving document to the current user's wallet. Card photos and the official PDF are attached separately."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201", description = "Created - Certification added",
                headers = [Header(name = "Location", description = "URL of the created certification", schema = Schema(type = "string"))],
                content = [Content(schema = Schema(implementation = CertificationDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid certification data",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Field validation error example",
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"courseName: must not be blank; issueDate: must be a date in the past or in the present\"}"
                    ), ExampleObject(
                        name = "CrossFieldErrorExample",
                        summary = "Cross-field validation error example",
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"agencyName: is required when agency is OTHER; expiryDate: must be after issueDate\"}"
                    ), ExampleObject(
                        name = "MalformedBodyExample",
                        summary = "Missing or invalid agency example",
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Malformed or incomplete request body\"}"
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
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping
    fun addMyCertification(@Valid @RequestBody request: CertificationRequestDTO): ResponseEntity<CertificationDTO> {
        val created = certificationService.createCertification(CurrentUser.id(), request)

        val location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{certificationId}")
            .buildAndExpand(created.id)
            .toUri()

        return ResponseEntity.created(location).body(created)
    }

    @Operation(
        summary = "Update certification",
        description = "Fully replace one of the current user's certifications. Optional fields sent as null are cleared. Attachments are not affected."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Updated certification",
                content = [Content(schema = Schema(implementation = CertificationDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid certification data or id",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "CrossFieldErrorExample",
                        summary = "Cross-field validation error example",
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"agencyName: is allowed only when agency is OTHER\"}"
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
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
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
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Certification not found\"}"
                    )]
                )]
            ),
        ]
    )
    @PutMapping("/{certificationId}")
    fun updateMyCertification(
        @Parameter(description = "Certification id", example = "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f")
        @PathVariable certificationId: UUID,
        @Valid @RequestBody request: CertificationRequestDTO,
    ): ResponseEntity<CertificationDTO> =
        ResponseEntity.ok(certificationService.updateCertification(CurrentUser.id(), certificationId, request))

    @Operation(summary = "Delete certification", description = "Permanently delete one of the current user's certifications.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Certification deleted",
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
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
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
                        value = "{\"timestamp\":\"2026-10-16T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Certification not found\"}"
                    )]
                )]
            ),
        ]
    )
    @DeleteMapping("/{certificationId}")
    fun deleteMyCertification(
        @Parameter(description = "Certification id", example = "9f1c2d3e-4b5a-4c6d-8e7f-0a1b2c3d4e5f")
        @PathVariable certificationId: UUID,
    ): ResponseEntity<Unit> {
        certificationService.deleteCertification(CurrentUser.id(), certificationId)
        return ResponseEntity.noContent().build()
    }
}