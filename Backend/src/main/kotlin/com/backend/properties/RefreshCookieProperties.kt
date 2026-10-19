package com.backend.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "refresh-cookie")
class RefreshCookieProperties {
    lateinit var name: String
    lateinit var path: String
    var secure: Boolean = false
    lateinit var sameSite: String
    val domain: String? = null
}