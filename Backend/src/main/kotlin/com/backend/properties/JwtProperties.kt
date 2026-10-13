package com.backend.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "jwt")
class JwtProperties {
    lateinit var secret: String
    var accessTokenTtlMinutes: Long = 15
    var refreshTokenTtlDays: Long = 30
}