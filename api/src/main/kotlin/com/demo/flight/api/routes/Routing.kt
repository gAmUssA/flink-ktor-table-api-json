package com.demo.flight.api.routes

import com.demo.flight.api.services.FlightEventService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.collectLatest
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.Instant
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

/**
 * Configure routing for the Flight Control Center API.
 */
fun Application.configureRouting() {
    val logger = KotlinLogging.logger {}
    
    // Create and start the flight event service
    val flightEventService = FlightEventService(
        bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:29092",
        topic = System.getenv("KAFKA_TOPIC") ?: "flight-events",
        groupId = System.getenv("KAFKA_GROUP_ID") ?: "api-server"
    )
    flightEventService.start()
    
    // Register shutdown hook to stop the service when the application stops
    environment.monitor.subscribe(ApplicationStopping) {
        logger.info { "Stopping FlightEventService due to application shutdown" }
        flightEventService.stop()
    }
    
    // Create JSON mapper for serializing events
    val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    routing {
        // Health check endpoint
        get("/health") {
            call.respond(mapOf("status" to "UP", "timestamp" to Instant.now().toString()))
        }
        
        // API routes
        route("/api") {
            // Flight density endpoint
            get("/flights/density") {
                // This is a placeholder for the actual implementation
                // In a real implementation, this would query the database for flight density data
                call.respond(
                    mapOf(
                        "timestamp" to Instant.now().toString(),
                        "data" to listOf(
                            mapOf("grid_lat" to 50.0, "grid_lon" to 8.0, "flight_count" to 5),
                            mapOf("grid_lat" to 51.0, "grid_lon" to 9.0, "flight_count" to 3)
                        )
                    )
                )
            }
            
            // Delayed flights endpoint
            get("/flights/delayed") {
                // This is a placeholder for the actual implementation
                // In a real implementation, this would query the database for delayed flights
                call.respond(
                    mapOf(
                        "timestamp" to Instant.now().toString(),
                        "data" to listOf(
                            mapOf(
                                "flightId" to "LH123",
                                "airline" to "Lufthansa",
                                "delayMinutes" to 20,
                                "origin" to "FRA",
                                "destination" to "JFK"
                            )
                        )
                    )
                )
            }
            
            // WebSocket for live flight updates
            webSocket("/flights/live") {
                val clientAddress = call.request.local.remoteHost
                logger.info { "WebSocket connection established from $clientAddress" }
                
                try {
                    // Collect flight events from the service and send them to the client
                    flightEventService.flightEvents.collectLatest { event ->
                        try {
                            // Serialize the event to JSON
                            val json = objectMapper.writeValueAsString(event)
                            send(Frame.Text(json))
                        } catch (e: Exception) {
                            logger.error(e) { "Error sending event to WebSocket" }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error in WebSocket connection" }
                } finally {
                    logger.info { "WebSocket connection closed from $clientAddress" }
                }
            }
        }
    }
}
