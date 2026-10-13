package com.backend.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "otp")
class OtpProperties {
    var ttlMinutes: Long = 30
    var maxAttempts: Long = 3
    var resendCooldownSeconds: Long = 60
}