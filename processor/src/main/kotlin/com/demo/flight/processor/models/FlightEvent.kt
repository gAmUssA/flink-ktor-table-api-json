package com.demo.flight.processor.models

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
 * Types of flight events that can be processed.
 */
enum class EventType {
    POSITION_UPDATE,
    DELAY_NOTIFICATION,
    DEPARTURE,
    ARRIVAL,
    CANCELLATION;
    
    companion object {
        /**
         * Parse event type from string, case-insensitive.
         * Returns POSITION_UPDATE as default if the string doesn't match any known type.
         */
        fun fromString(value: String): EventType {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                POSITION_UPDATE
            }
        }
    }
}
