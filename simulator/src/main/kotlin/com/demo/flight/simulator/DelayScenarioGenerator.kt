package com.demo.flight.simulator

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalTime
import java.time.ZoneId
import kotlin.random.Random

/**
 * Generates realistic delay scenarios for flights based on various factors.
 * This includes weather conditions, time of day, airport congestion, etc.
 */
class DelayScenarioGenerator(
    private val random: Random = Random(System.currentTimeMillis())
) {
    private val logger = KotlinLogging.logger {}
    
    // Airports with higher congestion probability
    private val congestedAirports = setOf("JFK", "LHR", "CDG", "FRA", "LAX", "ORD", "ATL")
    
    // Time zones for major regions
    private val airportTimeZones = mapOf(
        "FRA" to ZoneId.of("Europe/Berlin"),
        "LHR" to ZoneId.of("Europe/London"),
        "CDG" to ZoneId.of("Europe/Paris"),
        "AMS" to ZoneId.of("Europe/Amsterdam"),
        "MAD" to ZoneId.of("Europe/Madrid"),
        "FCO" to ZoneId.of("Europe/Rome"),
        "JFK" to ZoneId.of("America/New_York"),
        "LAX" to ZoneId.of("America/Los_Angeles"),
        "ORD" to ZoneId.of("America/Chicago"),
        "ATL" to ZoneId.of("America/New_York"),
        "DFW" to ZoneId.of("America/Chicago"),
        "MUC" to ZoneId.of("Europe/Berlin"),
        "BCN" to ZoneId.of("Europe/Madrid"),
        "ZRH" to ZoneId.of("Europe/Zurich"),
        "VIE" to ZoneId.of("Europe/Vienna"),
        "IST" to ZoneId.of("Europe/Istanbul")
    )
    
    // Weather conditions by region (simplified)
    private val weatherConditions = mapOf(
        "Europe/London" to WeatherPattern(0.25, 15, 45),
        "Europe/Paris" to WeatherPattern(0.20, 15, 40),
        "Europe/Berlin" to WeatherPattern(0.22, 20, 50),
        "Europe/Madrid" to WeatherPattern(0.10, 10, 30),
        "Europe/Rome" to WeatherPattern(0.15, 15, 35),
        "Europe/Amsterdam" to WeatherPattern(0.30, 20, 60),
        "Europe/Zurich" to WeatherPattern(0.25, 15, 45),
        "Europe/Vienna" to WeatherPattern(0.20, 15, 40),
        "Europe/Istanbul" to WeatherPattern(0.15, 10, 30),
        "America/New_York" to WeatherPattern(0.25, 30, 90),
        "America/Chicago" to WeatherPattern(0.30, 25, 80),
        "America/Los_Angeles" to WeatherPattern(0.15, 20, 60)
    )
    
    /**
     * Determines if a flight should be delayed and by how many minutes.
     * 
     * @param originAirport The origin airport code
     * @param destinationAirport The destination airport code
     * @param currentProgress The current progress of the flight (0.0 to 1.0)
     * @return A pair of (should delay, delay minutes) or null if no delay
     */
    fun generateDelayScenario(
        originAirport: String,
        destinationAirport: String,
        currentProgress: Double
    ): Pair<Boolean, Int>? {
        // Different delay factors based on flight phase
        return when {
            // Pre-departure delays (most common)
            currentProgress < 0.05 -> checkPreDepartureDelay(originAirport)
            
            // En-route delays (weather, air traffic)
            currentProgress in 0.05..0.85 -> checkEnRouteDelay(originAirport, destinationAirport)
            
            // Arrival delays (destination airport congestion)
            currentProgress > 0.85 -> checkArrivalDelay(destinationAirport)
            
            // No delay
            else -> null
        }
    }
    
    /**
     * Checks for pre-departure delays based on origin airport.
     */
    private fun checkPreDepartureDelay(originAirport: String): Pair<Boolean, Int>? {
        // Base probability for pre-departure delay - reduced to achieve 10% delayed flights
        var delayProbability = 0.04
        
        // Increase probability for congested airports - reduced to achieve 10% delayed flights
        if (originAirport in congestedAirports) {
            delayProbability += 0.03
        }
        
        // Check time of day at origin airport (rush hours have more delays)
        val airportTimeZone = airportTimeZones[originAirport] ?: ZoneId.of("UTC")
        val localTime = LocalTime.now(airportTimeZone)
        
        // Rush hours: 7-10 AM and 4-7 PM
        if ((localTime.hour in 7..10) || (localTime.hour in 16..19)) {
            delayProbability += 0.10
        }
        
        // Late night flights have fewer delays
        if (localTime.hour in 22..23 || localTime.hour in 0..5) {
            delayProbability -= 0.05
        }
        
        // Weather factor
        val weatherPattern = weatherConditions[airportTimeZone.id] ?: WeatherPattern(0.2, 15, 45)
        delayProbability += weatherPattern.delayProbability
        
        // Determine if delay occurs
        return if (random.nextDouble() < delayProbability) {
            // Generate delay duration (minutes)
            val delayMinutes = when {
                random.nextDouble() < 0.7 -> random.nextInt(15, 61) // 70% chance: 15-60 minutes
                random.nextDouble() < 0.9 -> random.nextInt(61, 121) // 20% chance: 61-120 minutes
                else -> random.nextInt(121, 241) // 10% chance: 121-240 minutes (severe delay)
            }
            
            logger.info { "Generated pre-departure delay of $delayMinutes minutes at $originAirport" }
            Pair(true, delayMinutes)
        } else {
            null
        }
    }
    
    /**
     * Checks for en-route delays based on weather and air traffic.
     */
    private fun checkEnRouteDelay(originAirport: String, destinationAirport: String): Pair<Boolean, Int>? {
        // En-route delays are less common than pre-departure delays - reduced to achieve 10% delayed flights
        var delayProbability = 0.015
        
        // Weather factors from both origin and destination regions
        val originTimeZone = airportTimeZones[originAirport] ?: ZoneId.of("UTC")
        val destTimeZone = airportTimeZones[destinationAirport] ?: ZoneId.of("UTC")
        
        val originWeather = weatherConditions[originTimeZone.id] ?: WeatherPattern(0.2, 15, 45)
        val destWeather = weatherConditions[destTimeZone.id] ?: WeatherPattern(0.2, 15, 45)
        
        // Average the weather probabilities
        delayProbability += (originWeather.delayProbability + destWeather.delayProbability) / 4
        
        // Determine if delay occurs
        return if (random.nextDouble() < delayProbability) {
            // En-route delays tend to be shorter
            val delayMinutes = when {
                random.nextDouble() < 0.8 -> random.nextInt(10, 31) // 80% chance: 10-30 minutes
                else -> random.nextInt(31, 61) // 20% chance: 31-60 minutes
            }
            
            logger.info { "Generated en-route delay of $delayMinutes minutes for flight from $originAirport to $destinationAirport" }
            Pair(true, delayMinutes)
        } else {
            null
        }
    }
    
    /**
     * Checks for arrival delays based on destination airport congestion.
     */
    private fun checkArrivalDelay(destinationAirport: String): Pair<Boolean, Int>? {
        // Base probability for arrival delay - reduced to achieve 10% delayed flights
        var delayProbability = 0.025
        
        // Increase probability for congested airports - reduced to achieve 10% delayed flights
        if (destinationAirport in congestedAirports) {
            delayProbability += 0.04
        }
        
        // Check time of day at destination airport
        val airportTimeZone = airportTimeZones[destinationAirport] ?: ZoneId.of("UTC")
        val localTime = LocalTime.now(airportTimeZone)
        
        // Rush hours: 7-10 AM and 4-7 PM
        if ((localTime.hour in 7..10) || (localTime.hour in 16..19)) {
            delayProbability += 0.07
        }
        
        // Weather factor
        val weatherPattern = weatherConditions[airportTimeZone.id] ?: WeatherPattern(0.2, 15, 45)
        delayProbability += weatherPattern.delayProbability / 2
        
        // Determine if delay occurs
        return if (random.nextDouble() < delayProbability) {
            // Generate delay duration (minutes)
            val delayMinutes = when {
                random.nextDouble() < 0.75 -> random.nextInt(10, 31) // 75% chance: 10-30 minutes
                else -> random.nextInt(31, 61) // 25% chance: 31-60 minutes
            }
            
            logger.info { "Generated arrival delay of $delayMinutes minutes at $destinationAirport" }
            Pair(true, delayMinutes)
        } else {
            null
        }
    }
    
    /**
     * Represents weather patterns for a region.
     */
    data class WeatherPattern(
        val delayProbability: Double, // Additional probability of delay due to weather
        val minDelayMinutes: Int,     // Minimum delay in minutes
        val maxDelayMinutes: Int      // Maximum delay in minutes
    )
}
