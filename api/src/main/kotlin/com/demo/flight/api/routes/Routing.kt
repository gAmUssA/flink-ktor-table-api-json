package com.demo.flight.api.routes

import com.demo.flight.api.services.DatabaseService
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
    
    // Create database service
    val databaseService = DatabaseService()
    
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
                logger.info { "Fetching flight density data" }
                val densityData = databaseService.getCurrentDensity()
                call.respond(densityData)
            }
            
            // Delayed flights endpoint
            get("/flights/delayed") {
                logger.info { "Fetching delayed flights data" }
                val delayedFlightsData = databaseService.getDelayedFlights()
                call.respond(delayedFlightsData)
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
