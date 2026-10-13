package com.backend.services

import com.backend.properties.JwtProperties
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

sealed interface AccessTokenParseResult {
    data class Valid(val userId: UUID) : AccessTokenParseResult
    data object Expired : AccessTokenParseResult
    data object Invalid : AccessTokenParseResult
}

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret))

    private val parser = Jwts.parser()
        .verifyWith(key)
        .build()

    fun generateAccessToken(userId: UUID, role: String): String {
        val now = Instant.now()
        val expiration = now.plus(jwtProperties.accessTokenTtlMinutes, ChronoUnit.MINUTES)

        val token = Jwts.builder()
            .subject(userId.toString())
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(key, Jwts.SIG.HS256)
            .compact()

        logger.debug("\n\t[DEBUG] [jwt_service][generate_access_token] Generated access token for user {} with role {}", userId, role)
        return token
    }

    fun parseAccessToken(token: String): AccessTokenParseResult {
        return try {
            val claims = parser.parseSignedClaims(token).payload
            AccessTokenParseResult.Valid(UUID.fromString(claims.subject))
        } catch(e: ExpiredJwtException) {
            logger.debug("\n\t[DEBUG] [jwt_service][parse_access_token] Expired token for subject {}", e.claims.subject)
            AccessTokenParseResult.Expired
        } catch(e: JwtException) {
            logger.warn("\n\t[WARN] [jwt_service][parse_access_token] Invalid token: {}", e.message)
            AccessTokenParseResult.Invalid
        } catch(_: IllegalArgumentException) {
            logger.warn("\n\t[WARN] [jwt_service][parse_access_token] Malformed or missing user id in token subject")
            AccessTokenParseResult.Invalid
        }
    }
}