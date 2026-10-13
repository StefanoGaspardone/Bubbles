package com.backend.security

const val AUTH_ERROR_ATTRIBUTE = "com.backend.security.AUTH_ERROR_REASON"

enum class AuthErrorReason {
    TOKEN_EXPIRED,
    TOKEN_INVALID,
    ACCOUNT_NOT_ACTIVATED,
    ACCOUNT_DISABLED,
}