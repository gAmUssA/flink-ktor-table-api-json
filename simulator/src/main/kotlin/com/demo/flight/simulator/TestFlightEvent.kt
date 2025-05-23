package com.demo.flight.simulator

import com.demo.flight.simulator.models.EventType
import com.demo.flight.simulator.models.FlightEvent
import java.time.Instant

fun main() {
    val event = FlightEvent(
        flightId = "LH123",
        airline = "Lufthansa",
        eventType = EventType.POSITION_UPDATE,
        timestamp = Instant.now(),
        latitude = 50.0379,
        longitude = 8.5622,
        delayMinutes = 0,
        origin = "FRA",
        destination = "JFK"
    )
    
    println("Created event: $event")
}
