package com.backend.controllers

import com.backend.configs.OpenAPIConfig
import com.backend.exceptions.ErrorResponse
import com.backend.models.dtos.DeleteAccountRequestDTO
import com.backend.models.dtos.UpdateUserRequestDTO
import com.backend.models.dtos.UserDTO
import com.backend.security.CurrentUser
import com.backend.services.UserService
import io.swagger.v3.oas.annotations.Operation
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
import org.springframework.web.bind.annotation.*

@Tag(name = "Users", description = "REST APIs for user management")
@SecurityRequirement(name = OpenAPIConfig.BEARER_AUTH)
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('USER')")
class UserController(
    private val userService: UserService,
) {

    @Operation(summary = "Get my profile", description = "Get the account information of the currently authenticated user.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Your account information",
                content = [Content(schema = Schema(implementation = UserDTO::class))]
            ),
            ApiResponse(
                responseCode = "401", description = "Unauthorized - Missing, invalid or expired access token",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "AuthenticationRequiredExample",
                        summary = "Missing token example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}"
                    ), ExampleObject(
                        name = "TokenExpiredExample",
                        summary = "Expired token example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
        ]
    )
    @GetMapping("/me")
    fun getMyProfile(): ResponseEntity<UserDTO> =
        ResponseEntity.ok(userService.getUserById(CurrentUser.id()))

    @Operation(
        summary = "Update my profile",
        description = "Update the current user's username, first name, last name and/or notifications preference. " +
                "Fields left null in the request are not modified."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Ok - Updated profile",
                content = [Content(schema = Schema(implementation = UserDTO::class))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Invalid profile data",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Validation error example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"username: Username can contain only letters, digits, dot and underscore\"}"
                    ), ExampleObject(
                        name = "BlankNameExample",
                        summary = "Blank first name example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"firstName: First name must not be blank\"}"
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
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "409", description = "Conflict - Username already taken",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "UsernameTakenExample",
                        summary = "Username already taken example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":409,\"error\":\"Conflict\",\"message\":\"Username already taken: johndoe2\"}"
                    )]
                )]
            ),
        ]
    )
    @PatchMapping("/me")
    fun updateMyProfile(@Valid @RequestBody request: UpdateUserRequestDTO): ResponseEntity<UserDTO> =
        ResponseEntity.ok(userService.updateProfile(CurrentUser.id(), request))

    @Operation(
        summary = "Delete my account",
        description = "Permanently anonymize the current user's account and revoke all its sessions. " +
                "Requires the current password. The client must discard its tokens afterwards."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204", description = "No Content - Account deleted",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "400", description = "Bad Request - Missing or incorrect password",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [ExampleObject(
                        name = "ValidationErrorExample",
                        summary = "Missing password example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"password: must not be blank\"}"
                    ), ExampleObject(
                        name = "IncorrectPasswordExample",
                        summary = "Incorrect password example",
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":400,\"error\":\"Bad Request\",\"message\":\"Incorrect password\"}"
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
                        value = "{\"timestamp\":\"2026-10-13T12:00:00Z\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Access token expired\"}"
                    )]
                )]
            ),
        ]
    )
    @PostMapping("/me/delete")
    fun deleteMyAccount(@Valid @RequestBody request: DeleteAccountRequestDTO): ResponseEntity<Unit> {
        userService.deleteAccount(CurrentUser.id(), request)
        return ResponseEntity.noContent().build()
    }
}