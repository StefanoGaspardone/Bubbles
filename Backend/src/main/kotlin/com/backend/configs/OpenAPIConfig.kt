package com.backend.configs

import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.net.InetAddress

@Configuration
class OpenAPIConfig(private val environment: Environment) {

    companion object {
        const val BEARER_AUTH = "bearerAuth"
    }

    init {
        ModelResolver.enumsAsRef = true
    }

    @Bean
    fun getOpenApiDocumentation(): OpenAPI {
        val port = environment.getProperty("server.port", "8080")
        val ip = InetAddress.getLocalHost().hostAddress

        return OpenAPI()
            .info(
                Info()
                    .title("Bubbles Backend API")
                    .version("1.0.0")
                    .description("API documentation for Bubbles Backend")
            )
            .components(
                Components().addSecuritySchemes(
                    BEARER_AUTH,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token returned by /api/v1/auth/login or /api/v1/auth/refresh")
                )
            )
            .addServersItem(Server().url("http://localhost:$port").description("Bubbles Backend local device"))
            .addServersItem(Server().url("http://$ip:$port").description("Bubbles Backend local network"))
    }
}