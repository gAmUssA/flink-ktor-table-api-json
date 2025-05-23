package com.demo.flight.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.Instant

/**
 * Configure routing for the Flight Control Center API.
 */
fun Application.configureRouting() {
    val logger = KotlinLogging.logger {}
    
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
                logger.info { "WebSocket connection established" }
                
                try {
                    // In a real implementation, this would subscribe to Kafka and forward messages
                    // For now, we'll simulate real-time flight data
                    val airlines = listOf("Lufthansa", "British Airways", "Air France", "KLM", "Emirates")
                    val origins = listOf("FRA", "LHR", "CDG", "AMS", "DXB")
                    val destinations = listOf("JFK", "LAX", "SFO", "ORD", "MIA")
                    val eventTypes = listOf("POSITION_UPDATE", "TAKEOFF", "LANDING", "DELAY_UPDATE")
                    val random = java.util.Random()
                    
                    // Keep the connection open and send continuous updates
                    while (true) {
                        val flightId = "${airlines[random.nextInt(airlines.size)].take(2)}${100 + random.nextInt(900)}"
                        val airline = airlines[random.nextInt(airlines.size)]
                        val eventType = eventTypes[random.nextInt(eventTypes.size)]
                        val origin = origins[random.nextInt(origins.size)]
                        val destination = destinations[random.nextInt(destinations.size)]
                        
                        // Generate random coordinates centered around Europe
                        val latitude = 50.0 + random.nextDouble(-10.0, 10.0)
                        val longitude = 8.0 + random.nextDouble(-15.0, 15.0)
                        val delayMinutes = if (random.nextDouble() < 0.3) random.nextInt(5, 60) else 0
                        
                        val message = """
                            {
                                "flightId": "$flightId",
                                "airline": "$airline",
                                "eventType": "$eventType",
                                "timestamp": "${Instant.now()}",
                                "latitude": $latitude,
                                "longitude": $longitude,
                                "delayMinutes": $delayMinutes,
                                "origin": "$origin",
                                "destination": "$destination"
                            }
                        """.trimIndent()
                        
                        send(Frame.Text(message))
                        kotlinx.coroutines.delay(1000) // Send update every second
                    }
                    
                } catch (e: Exception) {
                    logger.error(e) { "Error in WebSocket" }
                } finally {
                    logger.info { "WebSocket connection closed" }
                }
            }
        }
    }
}
