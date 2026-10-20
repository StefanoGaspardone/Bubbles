package com.backend.controllers

import com.backend.configs.OpenAPIConfig
import com.backend.exceptions.ErrorResponse
import com.backend.models.dtos.UserDTO
import com.backend.security.CurrentUser
import com.backend.services.UserService
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Users", description = "Manage the current user's account")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/users/me/avatar")
@PreAuthorize("hasRole('USER')")
class UserAvatarController(
    private val userService: UserService,
) {

    @Operation(
        summary = "Set my avatar",
        description = "Upload a new avatar for the current user, replacing the previous one (which is deleted). " +
                "Allowed formats: JPG, PNG, WEBP, up to 5 MB. The format is detected from the file content. " +
                "The avatar is public: use /api/v1/files/{avatarId}/content directly in <img src>."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Avatar updated, updated profile returned",
                content = [Content(schema = Schema(implementation = UserDTO::class))]
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
                    ), ExampleObject(
                        name = "EmptyFileExample",
                        summary = "Empty file example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"File is empty\"}"
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
                responseCode = "413", description = "Payload Too Large - File larger than 5 MB",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "TooLargeExample",
                        summary = "File too large example",
                        value = "{\"timestamp\":\"2026-10-20T12:00:00Z\",\"status\":413,\"error\":\"Payload Too Large\",\"message\":\"File is too large: maximum size is 5 MB\"}"
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
    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun setMyAvatar(
        @Parameter(description = "Avatar image (JPG, PNG or WEBP, max 5 MB)")
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<UserDTO> =
        ResponseEntity.ok(userService.setAvatar(CurrentUser.id(), file))

    @Operation(
        summary = "Remove my avatar",
        description = "Remove the current user's avatar and delete its file. Idempotent: returns 204 even if there is no avatar."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Avatar removed",
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
        ]
    )
    @DeleteMapping
    fun removeMyAvatar(): ResponseEntity<Unit> {
        userService.removeAvatar(CurrentUser.id())
        return ResponseEntity.noContent().build()
    }
}