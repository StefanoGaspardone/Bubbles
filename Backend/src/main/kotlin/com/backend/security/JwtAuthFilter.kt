package com.backend.security

import com.backend.models.enums.AccountStatus
import com.backend.repositories.UserRepository
import com.backend.services.AccessTokenParseResult
import com.backend.services.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
): OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val token = extractToken(request)

        if(token != null) {
            authenticate(token)?.let { request.setAttribute(AUTH_ERROR_ATTRIBUTE, it) }
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticate(token: String): AuthErrorReason? {
        val userId: UUID = when(val result = jwtService.parseAccessToken(token)) {
            is AccessTokenParseResult.Valid -> result.userId
            AccessTokenParseResult.Expired -> return AuthErrorReason.TOKEN_EXPIRED
            AccessTokenParseResult.Invalid -> return AuthErrorReason.TOKEN_INVALID
        }

        val authView = userRepository.findAuthViewById(userId)
            ?: run {
                log.warn("\n\t[WARN] [jwt_auth_filter][authenticate] Token for non-existent user {}", userId)
                return AuthErrorReason.TOKEN_INVALID
            }

        when(authView.accountStatus) {
            AccountStatus.ACTIVE -> Unit
            AccountStatus.PENDING_ACTIVATION -> return AuthErrorReason.ACCOUNT_NOT_ACTIVATED
            else -> {
                log.warn("\n\t[WARN] [jwt_auth_filter][authenticate] Token used by {} account {}", authView.accountStatus, userId)
                return AuthErrorReason.ACCOUNT_DISABLED
            }
        }

        val role = authView.role.name
        val principal = JwtUserPrincipal(userId = userId, role = role)
        val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, authorities)

        log.debug("\n\t[DEBUG] [jwt_auth_filter][authenticate] Authenticated user {} with role {}", userId, role)
        return null
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        return if(header.startsWith("Bearer ")) header.substring(7) else null
    }
}