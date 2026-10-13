package com.backend.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "mail")
class EmailProperties {
    lateinit var fromAddress: String
    lateinit var fromName: String
}