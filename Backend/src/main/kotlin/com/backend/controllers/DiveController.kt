package com.backend.controllers

import com.backend.configs.OpenAPIConfig
import com.backend.exceptions.ErrorResponse
import com.backend.models.dtos.DiveConfirmationDTO
import com.backend.models.dtos.DiveDTO
import com.backend.models.dtos.DiveRequestDTO
import com.backend.models.dtos.DiveSummaryDTO
import com.backend.models.dtos.PageDTO
import com.backend.security.CurrentUser
import com.backend.services.DiveService
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
import java.time.Instant
import java.util.UUID

@Tag(name = "Dives", description = "REST APIs to manage the current user's logbook: list, create, update and delete dives")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/dives")
@PreAuthorize("hasRole('USER')")
class DiveController(
    private val diveService: DiveService,
) {

    @Operation(
        summary = "List my dives",
        description = "List the current user's dives, most recent first, with optional filters. " +
                "The dive number is the chronological position among ALL the user's dives plus the user's offset, " +
                "so it does not change when filters are applied."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Ok - Page of dives (empty if none)"),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid paging or filter parameters",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidPagingExample",
                        summary = "Invalid paging example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"size: must be between 1 and 100\"}"
                    ), ExampleObject(
                        name = "InvalidRangeExample",
                        summary = "Invalid date range example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"from: must be before to\"}"
                    ), ExampleObject(
                        name = "InvalidParameterExample",
                        summary = "Malformed parameter example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"siteId: invalid value 'abc'\"}"
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
    fun listMyDives(
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size (1-100)", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Only dives at this site", example = "0b8a3c1e-1f2d-4c3b-9a8e-7d6f5e4c3b2a")
        @RequestParam(required = false) siteId: UUID?,
        @Parameter(description = "Only dives started at or after this instant (ISO-8601)", example = "2026-01-01T00:00:00Z")
        @RequestParam(required = false) from: Instant?,
        @Parameter(description = "Only dives started before this instant (ISO-8601)", example = "2027-01-01T00:00:00Z")
        @RequestParam(required = false) to: Instant?,
    ): PageDTO<DiveSummaryDTO> =
        diveService.listDives(CurrentUser.id(), page, size, siteId, from, to)

    @Operation(
        summary = "Get dive",
        description = "Get the full details of one of the current user's dives, including tanks with computed consumption, " +
                "gear as it was when the dive was logged, surface interval and decompression-sickness warning flag."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Dive details",
                content = [Content(schema = Schema(implementation = DiveDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid dive id",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidIdExample",
                        summary = "Invalid id example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"diveId: invalid value 'abc'\"}"
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
                responseCode = "404", description = "Not Found - Dive does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "DiveNotFoundExample",
                        summary = "Dive not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Dive not found\"}"
                    )]
                )]
            ),
        ]
    )
    @GetMapping("/{diveId}")
    fun getMyDive(
        @Parameter(description = "Dive id", example = "5d7e2a9c-3b4f-4e1a-8c6d-2f1e0a9b8c7d")
        @PathVariable diveId: UUID,
    ): ResponseEntity<DiveDTO> =
        ResponseEntity.ok(diveService.getDive(CurrentUser.id(), diveId))

    @Operation(
        summary = "Log a dive",
        description = "Add a dive to the current user's logbook. Times are given as local date-time at the dive site plus its time zone. " +
                "If avgDepthM is omitted and a profile is given, the average depth is computed from the profile. " +
                "Gear must reference the user's gear by gearId: a snapshot is saved with the dive."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201", description = "Created - Dive logged",
                headers = [Header(name = "Location", description = "URL of the created dive", schema = Schema(type = "string"))],
                content = [Content(schema = Schema(implementation = DiveDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid dive data",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Field validation error example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"maxDepthM: must be greater than 0.0; tanks[0].volumeL: must be greater than or equal to 0.5\"}"
                    ), ExampleObject(
                        name = "CrossFieldErrorExample",
                        summary = "Cross-field validation error example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"endLocal: must be after startLocal; avgDepthM: must not exceed maxDepthM; tanks[0].endPressureBar: must not exceed startPressureBar\"}"
                    ), ExampleObject(
                        name = "ProfileErrorExample",
                        summary = "Profile inconsistency example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"segments: total duration (40 min) does not match the dive duration (52 min)\"}"
                    ), ExampleObject(
                        name = "GearErrorExample",
                        summary = "Invalid gear selection example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"gear[1].gearId: gear not found\"}"
                    ), ExampleObject(
                        name = "TimeZoneErrorExample",
                        summary = "Invalid time zone example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"timeZone: invalid time zone 'Europe/Torino'\"}"
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
                responseCode = "404", description = "Not Found - Dive site does not exist or is private to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "SiteNotFoundExample",
                        summary = "Dive site not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Dive site not found\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "409", description = "Conflict - The dive overlaps another dive in the logbook",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "OverlapExample",
                        summary = "Overlapping dive example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":409,\"error\":\"Conflict\",\"message\":\"This dive overlaps another dive in your logbook\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping
    fun logDive(@Valid @RequestBody request: DiveRequestDTO): ResponseEntity<DiveDTO> {
        val created = diveService.createDive(CurrentUser.id(), request)

        val location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{diveId}")
            .buildAndExpand(created.id)
            .toUri()

        return ResponseEntity.created(location).body(created)
    }

    @Operation(
        summary = "Update dive",
        description = "Fully replace a dive. Optional fields sent as null are cleared and lists are replaced. " +
                "Tanks are recreated. For gear, use diveGearId to keep an existing entry with its original data, " +
                "or gearId to snapshot a piece of the user's gear now; entries not listed are removed."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Updated dive",
                content = [Content(schema = Schema(implementation = DiveDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid dive data or id",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "CrossFieldErrorExample",
                        summary = "Cross-field validation error example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"startLocal: must not be in the future\"}"
                    ), ExampleObject(
                        name = "GearEntryErrorExample",
                        summary = "Unknown dive gear entry example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"gear[0].diveGearId: entry not found in this dive\"}"
                    ), ExampleObject(
                        name = "GearSelectionErrorExample",
                        summary = "Invalid gear selection example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"gear[2].validSelection: exactly one of diveGearId and gearId must be set\"}"
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
                responseCode = "404", description = "Not Found - Dive or dive site not found",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "DiveNotFoundExample",
                        summary = "Dive not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Dive not found\"}"
                    ), ExampleObject(
                        name = "SiteNotFoundExample",
                        summary = "Dive site not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Dive site not found\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "409", description = "Conflict - The new times overlap another dive in the logbook",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "OverlapExample",
                        summary = "Overlapping dive example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":409,\"error\":\"Conflict\",\"message\":\"This dive overlaps another dive in your logbook\"}"
                    )]
                )]
            ),
        ]
    )
    @PutMapping("/{diveId}")
    fun updateMyDive(
        @Parameter(description = "Dive id", example = "5d7e2a9c-3b4f-4e1a-8c6d-2f1e0a9b8c7d")
        @PathVariable diveId: UUID,
        @Valid @RequestBody request: DiveRequestDTO,
    ): ResponseEntity<DiveDTO> =
        ResponseEntity.ok(diveService.updateDive(CurrentUser.id(), diveId, request))

    @Operation(
        summary = "Delete dive",
        description = "Permanently delete a dive with its tanks and gear entries. " +
                "The numbers of the following dives decrease automatically. The user's gear is not affected."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Dive deleted",
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
                responseCode = "404", description = "Not Found - Dive does not exist or belongs to another user",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "DiveNotFoundExample",
                        summary = "Dive not found example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Dive not found\"}"
                    )]
                )]
            ),
        ]
    )
    @DeleteMapping("/{diveId}")
    fun deleteMyDive(
        @Parameter(description = "Dive id", example = "5d7e2a9c-3b4f-4e1a-8c6d-2f1e0a9b8c7d")
        @PathVariable diveId: UUID,
    ): ResponseEntity<Unit> {
        diveService.deleteDive(CurrentUser.id(), diveId)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "Confirm dive as buddy",
        description = "Confirm, as buddy, a dive logged by another user. Called by the buddy's app after scanning " +
                "the QR code shown by the diver who logged the dive. Idempotent: confirming again keeps the original date."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Dive confirmed, summary of what was confirmed",
                content = [Content(schema = Schema(implementation = DiveConfirmationDTO::class))]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized - Missing, invalid or expired access token",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "TokenExpiredExample",
                        summary = "Expired token example",
                        value = "{\"timestamp\":\"2026-10-15T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - Dive does not exist or the current user is not one of its buddies",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "DiveNotFoundExample",
                        summary = "Not a buddy of this dive example",
                        value = "{\"timestamp\":\"2026-10-15T12:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"Dive not found\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/{diveId}/confirm")
    fun confirmDiveAsBuddy(
        @Parameter(description = "Dive id, read from the QR code", example = "5d7e2a9c-3b4f-4e1a-8c6d-2f1e0a9b8c7d")
        @PathVariable diveId: UUID,
    ): ResponseEntity<DiveConfirmationDTO> =
        ResponseEntity.ok(diveService.confirmDive(CurrentUser.id(), diveId))
}