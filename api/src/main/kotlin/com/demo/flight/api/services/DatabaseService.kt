package com.demo.flight.api.services

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

/**
 * Service for interacting with the PostgreSQL database.
 */
class DatabaseService {
    private val logger = KotlinLogging.logger {}
    private val dataSource: DataSource
    
    init {
        val config = HikariConfig().apply {
            jdbcUrl = System.getenv("JDBC_URL") ?: "jdbc:postgresql://localhost:5432/flightdemo"
            username = System.getenv("DB_USERNAME") ?: "postgres"
            password = System.getenv("DB_PASSWORD") ?: "postgres"
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 60000
            connectionTimeout = 30000
            maxLifetime = 1800000
        }
        
        dataSource = HikariDataSource(config)
        logger.info { "Database connection pool initialized" }
    }
    
    /**
     * Get current flight density data.
     */
    fun getCurrentDensity(): Map<String, Any> {
        val results = mutableListOf<Map<String, Any>>()
        
        dataSource.connection.use { connection ->
            val statement = connection.prepareStatement("""
                SELECT grid_lat, grid_lon, flight_count, window_start
                FROM flight_density
                ORDER BY window_start DESC
            """)
            
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                results.add(mapOf(
                    "grid_lat" to resultSet.getInt("grid_lat"),
                    "grid_lon" to resultSet.getInt("grid_lon"),
                    "flight_count" to resultSet.getLong("flight_count"),
                    "window_start" to resultSet.getTimestamp("window_start").toInstant().toString()
                ))
            }
        }
        
        return mapOf(
            "timestamp" to Instant.now().toString(),
            "data" to results
        )
    }
    
    /**
     * Get delayed flights data.
     */
    fun getDelayedFlights(): Map<String, Any> {
        val results = mutableListOf<Map<String, Any>>()
        
        dataSource.connection.use { connection ->
            val statement = connection.prepareStatement("""
                SELECT flight_id, airline, delay_minutes, origin, destination, timestamp
                FROM delayed_flights
                ORDER BY timestamp DESC
            """)
            
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                results.add(mapOf(
                    "flightId" to resultSet.getString("flight_id"),
                    "airline" to resultSet.getString("airline"),
                    "delayMinutes" to resultSet.getInt("delay_minutes"),
                    "origin" to resultSet.getString("origin"),
                    "destination" to resultSet.getString("destination"),
                    "timestamp" to resultSet.getTimestamp("timestamp").toInstant().toString()
                ))
            }
        }
        
        return mapOf(
            "timestamp" to Instant.now().toString(),
            "data" to results
        )
    }
    
    /**
     * Get active flights (updated in the last 5 minutes).
     */
    fun getActiveFlights(): Map<String, Any> {
        val results = mutableListOf<Map<String, Any>>()
        
        dataSource.connection.use { connection ->
            val statement = connection.prepareStatement("""
                SELECT * FROM active_flights
                ORDER BY updated_at DESC
            """)
            
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                results.add(mapFlightFromResultSet(resultSet))
            }
        }
        
        return mapOf(
            "timestamp" to Instant.now().toString(),
            "data" to results
        )
    }
    
    /**
     * Map a flight from a ResultSet to a Map.
     */
    private fun mapFlightFromResultSet(rs: ResultSet): Map<String, Any> {
        return mapOf(
            "flightId" to rs.getString("flight_id"),
            "airline" to rs.getString("airline"),
            "eventType" to rs.getString("event_type"),
            "timestamp" to rs.getTimestamp("timestamp").toInstant().toString(),
            "latitude" to rs.getDouble("latitude"),
            "longitude" to rs.getDouble("longitude"),
            "delayMinutes" to rs.getInt("delay_minutes"),
            "origin" to rs.getString("origin"),
            "destination" to rs.getString("destination"),
            "updatedAt" to rs.getTimestamp("updated_at").toInstant().toString()
        )
    }
}
