package com.demo.flight.simulator.models

import java.time.Instant

/**
 * Represents a flight event in the system.
 */
data class FlightEvent(
    val flightId: String,
    val airline: String,
    val eventType: EventType,
    val timestamp: Instant,
    val latitude: Double,
    val longitude: Double,
    val delayMinutes: Int,
    val origin: String,
    val destination: String
)

/**
 * Types of flight events that can be generated.
 */
enum class EventType {
    POSITION_UPDATE,
    DELAY_NOTIFICATION,
    DEPARTURE,
    ARRIVAL,
    CANCELLATION
}
