package com.backend.configs

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.net.InetAddress

@Configuration
class OpenAPIConfig(private val environment: Environment) {

    @Bean
    fun getOpenApiDocumentation(): OpenAPI {
        val port = environment.getProperty("server.port")
        val ip = InetAddress.getLocalHost().hostAddress

        return OpenAPI()
            .info(
                Info()
                    .title("Bubbles Backend API")
                    .version("1.0.0")
                    .description("API documentation for Bubbles Backend")
            )
            .addServersItem(Server().url("http://localhost:$port").description("BubblesBackend local device"))
            .addServersItem(Server().url("http://$ip:$port").description("BubblesBackend local network"))
    }
}