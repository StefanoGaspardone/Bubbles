package com.backend.security

import com.backend.properties.JwtProperties
import com.backend.properties.RefreshCookieProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

const val CLIENT_TYPE_HEADER = "X-Client-Type"

enum class ClientType {
    WEB,
    MOBILE,
}

@Component
class RefreshTokenCookies(
    private val cookieProperties: RefreshCookieProperties,
    private val jwtProperties: JwtProperties,
) {
    fun create(rawRefreshToken: String): ResponseCookie =
        builder(rawRefreshToken)
            .maxAge(Duration.ofDays(jwtProperties.refreshTokenTtlDays))
            .build()

    fun clear(): ResponseCookie =
        builder("")
            .maxAge(Duration.ZERO)
            .build()

    fun read(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == cookieProperties.name }
            ?.value
            ?.takeIf { it.isNotBlank() }

    private fun builder(value: String): ResponseCookie.ResponseCookieBuilder {
        var builder = ResponseCookie.from(cookieProperties.name, value)
            .httpOnly(true)
            .secure(cookieProperties.secure)
            .sameSite(cookieProperties.sameSite)
            .path(cookieProperties.path)

        cookieProperties.domain?.let { builder = builder.domain(it) }
        return builder
    }
}