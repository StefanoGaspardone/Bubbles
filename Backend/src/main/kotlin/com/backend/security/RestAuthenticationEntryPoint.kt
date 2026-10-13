package com.backend.security

import com.backend.exceptions.AccessTokenExpiredException
import com.backend.exceptions.AccountDisabledException
import com.backend.exceptions.AccountNotActivatedException
import com.backend.exceptions.AuthenticationRequiredException
import com.backend.exceptions.InvalidAccessTokenException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver

@Component
class RestAuthenticationEntryPoint(
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver,
): AuthenticationEntryPoint {

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        val exception: Exception = when(request.getAttribute(AUTH_ERROR_ATTRIBUTE) as? AuthErrorReason) {
            AuthErrorReason.TOKEN_EXPIRED -> AccessTokenExpiredException()
            AuthErrorReason.TOKEN_INVALID -> InvalidAccessTokenException()
            AuthErrorReason.ACCOUNT_NOT_ACTIVATED -> AccountNotActivatedException()
            AuthErrorReason.ACCOUNT_DISABLED -> AccountDisabledException()
            null -> AuthenticationRequiredException()
        }

        resolver.resolveException(request, response, null, exception)
    }
}