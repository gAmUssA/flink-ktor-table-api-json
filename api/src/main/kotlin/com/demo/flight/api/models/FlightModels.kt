package com.demo.flight.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

/**
 * Data class representing a flight event.
 */
data class FlightEvent(
    @JsonProperty("flightId")
    val flightId: String,
    
    @JsonProperty("airline")
    val airline: String,
    
    @JsonProperty("eventType")
    val eventType: String,
    
    @JsonProperty("timestamp")
    val timestamp: Instant,
    
    @JsonProperty("latitude")
    val latitude: Double,
    
    @JsonProperty("longitude")
    val longitude: Double,
    
    @JsonProperty("delayMinutes")
    val delayMinutes: Int,
    
    @JsonProperty("origin")
    val origin: String,
    
    @JsonProperty("destination")
    val destination: String
)

/**
 * Data class representing a delayed flight.
 */
data class DelayedFlight(
    @JsonProperty("flightId")
    val flightId: String,
    
    @JsonProperty("airline")
    val airline: String,
    
    @JsonProperty("delayMinutes")
    val delayMinutes: Int,
    
    @JsonProperty("origin")
    val origin: String,
    
    @JsonProperty("destination")
    val destination: String,
    
    @JsonProperty("timestamp")
    val timestamp: Instant
)

/**
 * Data class representing flight density in a grid cell.
 */
data class FlightDensity(
    @JsonProperty("grid_lat")
    val gridLat: Int,
    
    @JsonProperty("grid_lon")
    val gridLon: Int,
    
    @JsonProperty("flight_count")
    val flightCount: Long,
    
    @JsonProperty("window_start")
    val windowStart: Instant
)

/**
 * Data class for API responses with a timestamp and data.
 */
data class ApiResponse<T>(
    @JsonProperty("timestamp")
    val timestamp: Instant = Instant.now(),
    
    @JsonProperty("data")
    val data: T
)
