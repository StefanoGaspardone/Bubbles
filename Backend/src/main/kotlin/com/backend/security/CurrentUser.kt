package com.backend.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

object CurrentUser {

    fun id(): UUID {
        val principal = authenticationOrThrow().principal
        return (principal as? JwtUserPrincipal)?.userId
            ?: throw IllegalStateException("Current principal does not carry a user id")
    }

    fun idOrNull(): UUID? =
        (SecurityContextHolder.getContext().authentication?.principal as? JwtUserPrincipal)?.userId

    fun role(): String {
        val principal = authenticationOrThrow().principal
        return (principal as? JwtUserPrincipal)?.role
            ?: throw IllegalStateException("Current principal does not carry a role")
    }

    private fun authenticationOrThrow(): Authentication =
        SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication present in SecurityContext")
}