package com.backend.controllers

import com.backend.configs.OpenAPIConfig
import com.backend.exceptions.ErrorResponse
import com.backend.models.dtos.GearDTO
import com.backend.models.dtos.GearRequestDTO
import com.backend.models.enums.GearCategory
import com.backend.security.CurrentUser
import com.backend.services.GearService
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

@Tag(name = "Gear", description = "REST APIs to manage the current user's diving gear: list, create, update, archive and delete")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/gear")
@PreAuthorize("hasRole('USER')")
class GearController(
    private val gearService: GearService,
) {

    @Operation(
        summary = "List my gear",
        description = "List the current user's gear, ordered by category and name. By default archived gear is excluded; use includeArchived=true to include it."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - List of gear (empty if none)",
                content = [Content(array = ArraySchema(schema = Schema(implementation = GearDTO::class)))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid query parameter",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidCategoryExample",
                        summary = "Invalid category example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"category: invalid value 'HOODIE'\"}"
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
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
        ]
    )
    @GetMapping
    fun listMyGear(
        @Parameter(description = "Filter by category. Omit to list all categories.", example = "WETSUIT")
        @RequestParam(required = false) category: GearCategory?,
        @Parameter(description = "Whether to include archived gear", example = "false")
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
    ): ResponseEntity<List<GearDTO>> =
        ResponseEntity.ok(gearService.listGear(CurrentUser.id(), category, includeArchived))

    @Operation(summary = "Get gear", description = "Get a single piece of the current user's gear.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Gear details",
                content = [Content(schema = Schema(implementation = GearDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid gear id",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidIdExample",
                        summary = "Invalid id example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"gearId: invalid value 'abc'\"}"
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
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - Gear does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "GearNotFoundExample",
                        summary = "Gear not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Gear not found\"}"
                    )]
                )]
            ),
        ]
    )
    @GetMapping("/{gearId}")
    fun getMyGear(
        @Parameter(description = "Gear id", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        @PathVariable gearId: UUID,
    ): ResponseEntity<GearDTO> =
        ResponseEntity.ok(gearService.getGear(CurrentUser.id(), gearId))

    @Operation(
        summary = "Create gear",
        description = "Add a piece of gear to the current user's inventory. Category-specific attributes are validated against the chosen category."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201", description = "Created - Gear created",
                headers = [Header(name = "Location", description = "URL of the created gear", schema = Schema(type = "string"))],
                content = [Content(schema = Schema(implementation = GearDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid gear data or attributes",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"name: must not be blank; serviceIntervalMonths: must be less than or equal to 120\"}"
                    ), ExampleObject(
                        name = "InvalidAttributesExample",
                        summary = "Invalid attributes example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"attributes.thicknessMm: must be between 0.5 and 15; attributes.kg: not allowed for category WETSUIT\"}"
                    ), ExampleObject(
                        name = "MalformedBodyExample",
                        summary = "Missing or invalid category example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Malformed or incomplete request body\"}"
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
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping
    fun createMyGear(@Valid @RequestBody request: GearRequestDTO): ResponseEntity<GearDTO> {
        val created = gearService.createGear(CurrentUser.id(), request)

        val location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{gearId}")
            .buildAndExpand(created.id)
            .toUri()

        return ResponseEntity.created(location).body(created)
    }

    @Operation(
        summary = "Update gear",
        description = "Fully replace a piece of gear. All fields are overwritten: optional fields sent as null are cleared. Attributes are validated against the (possibly new) category. The archived state is not changed."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Updated gear",
                content = [Content(schema = Schema(implementation = GearDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid gear data, attributes or id",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"purchaseDate: must be a date in the past or in the present\"}"
                    ), ExampleObject(
                        name = "InvalidAttributesExample",
                        summary = "Invalid attributes example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"attributes.thicknessMm: not allowed for category DRYSUIT\"}"
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
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - Gear does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "GearNotFoundExample",
                        summary = "Gear not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Gear not found\"}"
                    )]
                )]
            ),
        ]
    )
    @PutMapping("/{gearId}")
    fun updateMyGear(
        @Parameter(description = "Gear id", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        @PathVariable gearId: UUID,
        @Valid @RequestBody request: GearRequestDTO,
    ): ResponseEntity<GearDTO> =
        ResponseEntity.ok(gearService.updateGear(CurrentUser.id(), gearId, request))

    @Operation(
        summary = "Archive gear",
        description = "Mark a piece of gear as no longer in use (sold, broken...). Archived gear is not proposed for new dives but stays linked to past ones. Idempotent: archiving already archived gear keeps the original archive date."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Archived gear",
                content = [Content(schema = Schema(implementation = GearDTO::class))]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized - Missing, invalid or expired access token",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "TokenExpiredExample",
                        summary = "Expired token example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - Gear does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "GearNotFoundExample",
                        summary = "Gear not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Gear not found\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/{gearId}/archive")
    fun archiveMyGear(
        @Parameter(description = "Gear id", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        @PathVariable gearId: UUID,
    ): ResponseEntity<GearDTO> =
        ResponseEntity.ok(gearService.archiveGear(CurrentUser.id(), gearId))

    @Operation(
        summary = "Restore gear",
        description = "Mark archived gear as in use again. Idempotent: restoring gear already in use has no effect."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Restored gear",
                content = [Content(schema = Schema(implementation = GearDTO::class))]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized - Missing, invalid or expired access token",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "TokenExpiredExample",
                        summary = "Expired token example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - Gear does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "GearNotFoundExample",
                        summary = "Gear not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Gear not found\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/{gearId}/restore")
    fun restoreMyGear(
        @Parameter(description = "Gear id", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        @PathVariable gearId: UUID,
    ): ResponseEntity<GearDTO> =
        ResponseEntity.ok(gearService.restoreGear(CurrentUser.id(), gearId))

    @Operation(
        summary = "Delete gear",
        description = "Permanently delete a piece of gear. Past dives keep their copy of the gear as it was when the dive was logged. To keep the gear linked to past dives for statistics, archive it instead."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Gear deleted",
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
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - Gear does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "GearNotFoundExample",
                        summary = "Gear not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Gear not found\"}"
                    )]
                )]
            ),
        ]
    )
    @DeleteMapping("/{gearId}")
    fun deleteMyGear(
        @Parameter(description = "Gear id", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        @PathVariable gearId: UUID,
    ): ResponseEntity<Unit> {
        gearService.deleteGear(CurrentUser.id(), gearId)
        return ResponseEntity.noContent().build()
    }
}