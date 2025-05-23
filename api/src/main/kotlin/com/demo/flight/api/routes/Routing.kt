package com.demo.flight.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

/**
 * Configure routing for the Flight Control Center API.
 */
fun Application.configureRouting() {
    val logger = LoggerFactory.getLogger("Routing")
    
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
                logger.info("WebSocket connection established")
                
                try {
                    // This is a placeholder for the actual implementation
                    // In a real implementation, this would subscribe to Kafka and forward messages
                    for (i in 1..5) {
                        val message = """
                            {
                                "flightId": "LH${100 + i}",
                                "airline": "Lufthansa",
                                "eventType": "POSITION_UPDATE",
                                "timestamp": "${Instant.now()}",
                                "latitude": ${50.0 + i * 0.1},
                                "longitude": ${8.0 + i * 0.1},
                                "delayMinutes": ${i * 5},
                                "origin": "FRA",
                                "destination": "JFK"
                            }
                        """.trimIndent()
                        
                        send(Frame.Text(message))
                        kotlinx.coroutines.delay(1000)
                    }
                    
                    // Listen for incoming messages (client to server)
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            logger.info("Received message: ${frame.readText()}")
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error in WebSocket", e)
                } finally {
                    logger.info("WebSocket connection closed")
                }
            }
        }
    }
}
