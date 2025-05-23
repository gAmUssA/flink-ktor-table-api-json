package com.demo.flight.api

import com.demo.flight.api.routes.configureRouting
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.websocket.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.event.Level
import java.time.Duration

/**
 * Main application entry point for the Flight Control Center API.
 */
fun main() {
    embeddedServer(Netty, port = 8090, host = "0.0.0.0") {
        configureKtor()
    }.start(wait = true)
}

/**
 * Configure Ktor server features.
 */
fun Application.configureKtor() {
    val logger = KotlinLogging.logger {}
    
    // Install plugins
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }
    
    install(CORS) {
        // Allow requests from our frontend running on port 9000
        allowHost("localhost:9000", schemes = listOf("http", "https"))
        // Also allow any host for development purposes
        anyHost()
        // Allow common headers
        allowHeader("Content-Type")
        allowHeader("Authorization")
        // Allow all HTTP methods
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        // Allow credentials (cookies, etc.)
        allowCredentials = true
        // Allow WebSocket connections
        allowNonSimpleContentTypes = true
    }
    
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    install(CallLogging) {
        level = Level.INFO
    }
    
    install(StatusPages) {
        exception<Throwable> { _, cause ->
            logger.error(cause) { "Unhandled exception" }
            // Handle exceptions and return appropriate responses
        }
    }
    
    // Configure routes
    configureRouting()
    
    logger.info { "Flight Control Center API started" }
}
