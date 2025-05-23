package com.demo.flight.simulator.models

import java.time.Instant

/**
 * A simplified version of FlightEvent for testing
 */
data class SimpleFlightEvent(
    val flightId: String,
    val timestamp: Instant
)
