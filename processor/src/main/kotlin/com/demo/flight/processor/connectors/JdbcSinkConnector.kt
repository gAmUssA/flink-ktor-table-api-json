package com.demo.flight.processor.connectors

/**
 * Connector for JDBC sink operations.
 * Used to write data to PostgreSQL database.
 */
class JdbcSinkConnector {
    
    /**
     * Data class representing flight density in a geographic grid cell.
     */
    data class FlightDensity(
        val gridLat: Double,
        val gridLon: Double,
        val flightCount: Double
    )
}