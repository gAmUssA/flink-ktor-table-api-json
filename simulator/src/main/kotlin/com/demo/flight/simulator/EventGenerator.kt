package com.demo.flight.simulator

import com.demo.flight.simulator.models.EventType
import com.demo.flight.simulator.models.FlightEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.util.Properties
import java.io.FileInputStream
import kotlin.math.*
import kotlin.random.Random

/**
 * Generates realistic flight events for simulation purposes.
 * Handles flight path generation, delay scenarios, and other event types.
 */
class EventGenerator(
    private val random: Random = Random(System.currentTimeMillis()),
    private val configPath: String? = null
) {
    // Configuration properties
    private val config = Properties().apply {
        configPath?.let { path ->
            try {
                FileInputStream(path).use { this.load(it) }
                logger.info { "Loaded configuration from $path" }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to load configuration from $path" }
            }
        }
    }
    
    // Configurable parameters with defaults
    private val minFlights = config.getProperty("min.flights", "5").toIntOrNull() ?: 5
    private val maxFlights = config.getProperty("max.flights", "20").toIntOrNull() ?: 20
    private val delayProbability = config.getProperty("delay.probability", "0.05").toDoubleOrNull() ?: 0.05
    private val cancellationProbability = config.getProperty("cancellation.probability", "0.005").toDoubleOrNull() ?: 0.005
    
    // Delay scenario generator for realistic delays
    private val delayScenarioGenerator = DelayScenarioGenerator(random)
    private val logger = KotlinLogging.logger {}
    
    // Major European and North American airports with their coordinates
    private val airports = mapOf(
        "FRA" to Airport("Frankfurt", 50.0379, 8.5622),
        "LHR" to Airport("London Heathrow", 51.4700, -0.4543),
        "CDG" to Airport("Paris Charles de Gaulle", 49.0097, 2.5479),
        "AMS" to Airport("Amsterdam Schiphol", 52.3105, 4.7683),
        "MAD" to Airport("Madrid Barajas", 40.4983, -3.5676),
        "FCO" to Airport("Rome Fiumicino", 41.8045, 12.2508),
        "JFK" to Airport("New York JFK", 40.6413, -73.7781),
        "LAX" to Airport("Los Angeles", 33.9416, -118.4085),
        "ORD" to Airport("Chicago O'Hare", 41.9742, -87.9073),
        "ATL" to Airport("Atlanta", 33.6407, -84.4277),
        "DFW" to Airport("Dallas/Fort Worth", 32.8998, -97.0403),
        "MUC" to Airport("Munich", 48.3537, 11.7750),
        "BCN" to Airport("Barcelona", 41.2971, 2.0785),
        "ZRH" to Airport("Zurich", 47.4647, 8.5492),
        "VIE" to Airport("Vienna", 48.1102, 16.5697),
        "IST" to Airport("Istanbul", 41.2606, 28.7425)
    )
    
    // Major airlines with their IATA codes
    private val airlines = mapOf(
        "LH" to "Lufthansa",
        "BA" to "British Airways",
        "AF" to "Air France",
        "KL" to "KLM",
        "IB" to "Iberia",
        "AZ" to "Alitalia",
        "AA" to "American Airlines",
        "DL" to "Delta Air Lines",
        "UA" to "United Airlines",
        "LX" to "SWISS",
        "OS" to "Austrian Airlines",
        "TK" to "Turkish Airlines"
    )
    
    // Active flights being simulated with their current positions and statuses
    private val activeFlights = mutableMapOf<String, FlightStatus>()
    
    /**
     * Generates a new flight event.
     * This could be a new flight, an update to an existing flight, or a special event.
     */
    fun generateEvent(): FlightEvent {
        // Decide whether to update an existing flight or create a new one
        return if (activeFlights.isEmpty() || random.nextDouble() < 0.1) {
            createNewFlight()
        } else {
            updateExistingFlight()
        }
    }
    
    /**
     * Creates a new flight with random origin and destination.
     */
    private fun createNewFlight(): FlightEvent {
        // Select random origin and destination airports
        val originCode = airports.keys.random(random)
        var destinationCode: String
        do {
            destinationCode = airports.keys.random(random)
        } while (destinationCode == originCode)
        
        val origin = airports[originCode]!!
        val destination = airports[destinationCode]!!
        
        // Select random airline and create flight number
        val airlineCode = airlines.keys.random(random)
        val flightNumber = random.nextInt(100, 1000)
        val flightId = "$airlineCode$flightNumber"
        
        // Initialize flight status
        val status = FlightStatus(
            originCode = originCode,
            destinationCode = destinationCode,
            departureTime = Instant.now(),
            estimatedArrivalTime = Instant.now().plusSeconds((3600 * 2 + random.nextInt(3600 * 4)).toLong()),
            progress = 0.0,
            currentLatitude = origin.latitude,
            currentLongitude = origin.longitude,
            targetLatitude = destination.latitude,
            targetLongitude = destination.longitude,
            delayMinutes = 0
        )
        
        activeFlights[flightId] = status
        
        logger.info { "Created new flight: $flightId from $originCode to $destinationCode" }
        
        // Return a DEPARTURE event
        return FlightEvent(
            flightId = flightId,
            airline = airlines[airlineCode]!!,
            eventType = EventType.DEPARTURE,
            timestamp = Instant.now(),
            latitude = origin.latitude,
            longitude = origin.longitude,
            delayMinutes = 0,
            origin = originCode,
            destination = destinationCode
        )
    }
    
    /**
     * Updates an existing flight's position or status.
     */
    private fun updateExistingFlight(): FlightEvent {
        val flightId = activeFlights.keys.random(random)
        val status = activeFlights[flightId]!!
        
        // Extract airline code from flight ID
        val airlineCode = flightId.take(2)
        val airline = airlines[airlineCode] ?: "Unknown Airline"
        
        // Decide what type of event to generate
        return when {
            // Flight completed - generate ARRIVAL event
            status.progress >= 1.0 -> {
                val event = FlightEvent(
                    flightId = flightId,
                    airline = airline,
                    eventType = EventType.ARRIVAL,
                    timestamp = Instant.now(),
                    latitude = status.targetLatitude,
                    longitude = status.targetLongitude,
                    delayMinutes = status.delayMinutes,
                    origin = status.originCode,
                    destination = status.destinationCode
                )
                
                // Remove flight from active flights
                activeFlights.remove(flightId)
                logger.info { "Flight $flightId arrived at ${status.destinationCode}" }
                
                event
            }
            
            // Check for delay using the delay scenario generator
            status.delayMinutes == 0 -> {
                // Get delay scenario based on flight phase
                val delayScenario = delayScenarioGenerator.generateDelayScenario(
                    originAirport = status.originCode,
                    destinationAirport = status.destinationCode,
                    currentProgress = status.progress
                )
                
                if (delayScenario != null && delayScenario.first) {
                    // Apply the delay
                    val delayMinutes = delayScenario.second
                    status.delayMinutes = delayMinutes
                    
                    FlightEvent(
                        flightId = flightId,
                        airline = airline,
                        eventType = EventType.DELAY_NOTIFICATION,
                        timestamp = Instant.now(),
                        latitude = status.currentLatitude,
                        longitude = status.currentLongitude,
                        delayMinutes = delayMinutes,
                        origin = status.originCode,
                        destination = status.destinationCode
                    )
                } else {
                    // No delay, continue with position update
                    updateFlightPosition(status)
                    generatePositionUpdateEvent(flightId, airline, status)
                }
            }
            
            // Cancellation (configurable probability)
            random.nextDouble() < cancellationProbability -> {
                val event = FlightEvent(
                    flightId = flightId,
                    airline = airline,
                    eventType = EventType.CANCELLATION,
                    timestamp = Instant.now(),
                    latitude = status.currentLatitude,
                    longitude = status.currentLongitude,
                    delayMinutes = status.delayMinutes,
                    origin = status.originCode,
                    destination = status.destinationCode
                )
                
                // Remove flight from active flights
                activeFlights.remove(flightId)
                logger.info { "Flight $flightId cancelled" }
                
                event
            }
            
            // Regular position update (most common)
            else -> {
                // Calculate new position along great circle path
                updateFlightPosition(status)
                
                // Generate position update event
                generatePositionUpdateEvent(flightId, airline, status)
            }
        }
    }
    
    /**
     * Generates a position update event for a flight.
     */
    private fun generatePositionUpdateEvent(flightId: String, airline: String, status: FlightStatus): FlightEvent {
        return FlightEvent(
            flightId = flightId,
            airline = airline,
            eventType = EventType.POSITION_UPDATE,
            timestamp = Instant.now(),
            latitude = status.currentLatitude,
            longitude = status.currentLongitude,
            delayMinutes = status.delayMinutes,
            origin = status.originCode,
            destination = status.destinationCode
        )
    }
    
    /**
     * Updates a flight's position along a great circle path between origin and destination.
     * This creates a more realistic curved flight path over the Earth's surface.
     */
    private fun updateFlightPosition(status: FlightStatus) {
        // Use great circle calculations for realistic flight paths
        // Earth radius in kilometers
        val earthRadius = 6371.0
        
        // Convert coordinates from degrees to radians
        val startLat = Math.toRadians(status.currentLatitude)
        val startLon = Math.toRadians(status.currentLongitude)
        val endLat = Math.toRadians(status.targetLatitude)
        val endLon = Math.toRadians(status.targetLongitude)
        
        // Calculate the distance and bearing between current position and destination
        val distance = calculateDistance(startLat, startLon, endLat, endLon, earthRadius)
        val bearing = calculateBearing(startLat, startLon, endLat, endLon)
        
        // Calculate the distance to move in this update (based on progress)
        // We want to move faster in the middle of the flight and slower at takeoff/landing
        val progressFactor = if (status.progress < 0.2 || status.progress > 0.8) {
            // Slower at takeoff and landing
            0.005 + random.nextDouble(0.001, 0.003)
        } else {
            // Faster during cruise
            0.01 + random.nextDouble(0.005, 0.015)
        }
        
        // Update progress
        status.progress = minOf(1.0, status.progress + progressFactor)
        
        // If we're very close to destination or at destination, just arrive
        if (distance < 50 || status.progress >= 1.0) {
            status.currentLatitude = status.targetLatitude
            status.currentLongitude = status.targetLongitude
            status.progress = 1.0
            return
        }
        
        // Calculate new position
        val newPosition = calculateNewPosition(startLat, startLon, bearing, progressFactor * distance)
        
        // Update current position
        status.currentLatitude = Math.toDegrees(newPosition.first)
        status.currentLongitude = Math.toDegrees(newPosition.second)
        
        // Add some randomness to simulate weather and other factors (smaller at higher altitudes)
        val weatherFactor = if (status.progress > 0.2 && status.progress < 0.8) 0.01 else 0.03
        status.currentLatitude += random.nextDouble(-weatherFactor, weatherFactor)
        status.currentLongitude += random.nextDouble(-weatherFactor, weatherFactor)
    }
    
    /**
     * Calculates the distance between two points on the Earth's surface using the Haversine formula.
     * @return Distance in kilometers
     */
    private fun calculateDistance(
        startLat: Double, startLon: Double, 
        endLat: Double, endLon: Double, 
        radius: Double
    ): Double {
        val dLat = endLat - startLat
        val dLon = endLon - startLon
        
        val a = sin(dLat / 2).pow(2) + 
                cos(startLat) * cos(endLat) * 
                sin(dLon / 2).pow(2)
        
        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1.0 - a))
        
        return radius * c
    }
    
    /**
     * Calculates the initial bearing from start point to end point.
     * @return Bearing in radians
     */
    private fun calculateBearing(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double
    ): Double {
        val dLon = endLon - startLon
        
        val y = sin(dLon) * cos(endLat)
        val x = cos(startLat) * sin(endLat) - 
                sin(startLat) * cos(endLat) * cos(dLon)
        
        return kotlin.math.atan2(y, x)
    }
    
    /**
     * Calculates a new position given a starting point, bearing, and distance.
     * @return Pair of (latitude, longitude) in radians
     */
    private fun calculateNewPosition(
        startLat: Double, startLon: Double, 
        bearing: Double, distance: Double
    ): Pair<Double, Double> {
        val earthRadius = 6371.0 // kilometers
        
        val distRatio = distance / earthRadius
        
        val newLat = asin(
            sin(startLat) * cos(distRatio) + 
            cos(startLat) * sin(distRatio) * cos(bearing)
        )
        
        val newLon = startLon + kotlin.math.atan2(
            sin(bearing) * sin(distRatio) * cos(startLat),
            cos(distRatio) - sin(startLat) * sin(newLat)
        )
        
        return Pair(newLat, newLon)
    }
    
    /**
     * Represents an airport with its name and coordinates.
     */
    data class Airport(
        val name: String,
        val latitude: Double,
        val longitude: Double
    )
    
    /**
     * Tracks the current status of an active flight.
     */
    data class FlightStatus(
        val originCode: String,
        val destinationCode: String,
        val departureTime: Instant,
        val estimatedArrivalTime: Instant,
        var progress: Double,
        var currentLatitude: Double,
        var currentLongitude: Double,
        val targetLatitude: Double,
        val targetLongitude: Double,
        var delayMinutes: Int
    )
}
