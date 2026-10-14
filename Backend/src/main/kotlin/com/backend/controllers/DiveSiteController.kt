package com.backend.controllers

import com.backend.configs.OpenAPIConfig
import com.backend.exceptions.ErrorResponse
import com.backend.models.dtos.DiveSiteDTO
import com.backend.models.dtos.DiveSiteRequestDTO
import com.backend.models.dtos.PageDTO
import com.backend.models.enums.DiveSiteScope
import com.backend.models.enums.DiveSiteType
import com.backend.security.CurrentUser
import com.backend.services.DiveSiteService
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
import java.util.UUID

@Tag(name = "Dive sites", description = "REST APIs to search dive sites and manage the current user's personal sites")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/dive-sites")
@PreAuthorize("hasRole('USER')")
class DiveSiteController(
    private val diveSiteService: DiveSiteService,
) {

    @Operation(
        summary = "Search dive sites",
        description = "Search the dive sites visible to the current user (their own sites and public ones), ordered by name. " +
                "The text query matches name or locality, case-insensitively. " +
                "Each site includes how many dives the current user logged there."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Ok - Page of dive sites (empty if none)"),
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
                        name = "InvalidParameterExample",
                        summary = "Invalid enum parameter example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"siteType: invalid value 'BEACH'\"}"
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
    fun searchDiveSites(
        @Parameter(description = "Zero-based page index", example = "0")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size (1-100)", example = "20")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Text to search in name or locality", example = "portofino")
        @RequestParam(required = false) query: String?,
        @Parameter(description = "Only sites of this type", example = "WRECK")
        @RequestParam(required = false) siteType: DiveSiteType?,
        @Parameter(description = "ALL: own and public sites; MINE: only own sites; PUBLIC: only public sites", example = "ALL")
        @RequestParam(defaultValue = "ALL") scope: DiveSiteScope,
    ): PageDTO<DiveSiteDTO> =
        diveSiteService.listSites(CurrentUser.id(), page, size, query, siteType, scope)

    @Operation(
        summary = "Get dive site",
        description = "Get a dive site visible to the current user, with the number of dives they logged there."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Dive site details",
                content = [Content(schema = Schema(implementation = DiveSiteDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid site id",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "InvalidIdExample",
                        summary = "Invalid id example",
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
            ApiResponse(
                responseCode = "404", description = "Not Found - Site does not exist or is private to another user",
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
        ]
    )
    @GetMapping("/{siteId}")
    fun getDiveSite(
        @Parameter(description = "Dive site id", example = "0b8a3c1e-1f2d-4c3b-9a8e-7d6f5e4c3b2a")
        @PathVariable siteId: UUID,
    ): ResponseEntity<DiveSiteDTO> =
        ResponseEntity.ok(diveSiteService.getSite(CurrentUser.id(), siteId))

    @Operation(
        summary = "Create dive site",
        description = "Create a personal dive site, visible only to the current user."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201", description = "Created - Dive site created",
                headers = [Header(name = "Location", description = "URL of the created dive site", schema = Schema(type = "string"))],
                content = [Content(schema = Schema(implementation = DiveSiteDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid site data",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Field validation error example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"name: must not be blank; latitude: must be less than or equal to 90.0\"}"
                    ), ExampleObject(
                        name = "CrossFieldErrorExample",
                        summary = "Cross-field validation error example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"minDepthM: must not exceed maxDepthM; countryCode: unknown ISO country code 'XX'\"}"
                    ), ExampleObject(
                        name = "MalformedBodyExample",
                        summary = "Missing coordinates example",
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
    fun createDiveSite(@Valid @RequestBody request: DiveSiteRequestDTO): ResponseEntity<DiveSiteDTO> {
        val created = diveSiteService.createSite(CurrentUser.id(), request)

        val location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{siteId}")
            .buildAndExpand(created.id)
            .toUri()

        return ResponseEntity.created(location).body(created)
    }

    @Operation(
        summary = "Update dive site",
        description = "Fully replace one of the current user's personal sites. Optional fields sent as null are cleared. " +
                "Public sites cannot be modified. Dives linked to the site show the updated data."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Updated dive site",
                content = [Content(schema = Schema(implementation = DiveSiteDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid site data or id",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "CrossFieldErrorExample",
                        summary = "Cross-field validation error example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"minDepthM: must not exceed maxDepthM\"}"
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
                responseCode = "403", description = "Forbidden - Public sites cannot be modified",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "NotEditableExample",
                        summary = "Public site example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Public dive sites cannot be modified or deleted\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - Site does not exist or is private to another user",
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
        ]
    )
    @PutMapping("/{siteId}")
    fun updateDiveSite(
        @Parameter(description = "Dive site id", example = "0b8a3c1e-1f2d-4c3b-9a8e-7d6f5e4c3b2a")
        @PathVariable siteId: UUID,
        @Valid @RequestBody request: DiveSiteRequestDTO,
    ): ResponseEntity<DiveSiteDTO> =
        ResponseEntity.ok(diveSiteService.updateSite(CurrentUser.id(), siteId, request))

    @Operation(
        summary = "Delete dive site",
        description = "Delete one of the current user's personal sites. Not allowed while dives are linked to it: " +
                "move those dives to another site or delete them first. Public sites cannot be deleted."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Dive site deleted",
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
                responseCode = "403", description = "Forbidden - Public sites cannot be deleted",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "NotEditableExample",
                        summary = "Public site example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Public dive sites cannot be modified or deleted\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "404", description = "Not Found - Site does not exist or is private to another user",
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
                responseCode = "409", description = "Conflict - Dives are linked to the site",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "SiteInUseExample",
                        summary = "Site in use example",
                        value = "{\"timestamp\":\"2026-10-14T12:00:00Z\",\"status\":409,\"error\":\"Conflict\",\"message\":\"Dive site is used by 3 dives and cannot be deleted\"}"
                    )]
                )]
            ),
        ]
    )
    @DeleteMapping("/{siteId}")
    fun deleteDiveSite(
        @Parameter(description = "Dive site id", example = "0b8a3c1e-1f2d-4c3b-9a8e-7d6f5e4c3b2a")
        @PathVariable siteId: UUID,
    ): ResponseEntity<Unit> {
        diveSiteService.deleteSite(CurrentUser.id(), siteId)
        return ResponseEntity.noContent().build()
    }
}