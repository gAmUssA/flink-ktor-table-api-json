package com.demo.flight.processor.jobs

import com.demo.flight.processor.connectors.JdbcSinkConnector
import com.demo.flight.processor.models.FlightEvent
import org.apache.flink.api.common.eventtime.WatermarkStrategy
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer
import org.apache.flink.streaming.api.datastream.DataStream
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.Schema
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import java.time.Duration

/**
 * Flink job for aggregating flight density by geographic grid.
 * Uses the Table API with SQL to process flight events and calculate density.
 */
open class DensityAggregationJob : FlinkJobBase("density-aggregation-job", true) {

    // Grid size in degrees (approximately 50km at the equator)
    private val gridSize = 0.5

    /**
     * Calculate flight density from a stream of flight events.
     * Groups flights by grid cell and counts them.
     */
    protected fun calculateFlightDensity(events: DataStream<FlightEvent>): DataStream<JdbcSinkConnector.FlightDensity> {
        // Map each flight event to a grid cell
        return events.map { event ->
            // Round coordinates to grid
            val gridLat = Math.floor(event.latitude / gridSize) * gridSize
            val gridLon = Math.floor(event.longitude / gridSize) * gridSize

            // Create a FlightDensity object with count 1 for this event
            JdbcSinkConnector.FlightDensity(gridLat, gridLon, 1.0)
        }
    }

    override fun execute() {
        logger.info { "Executing $jobName" }

        // Create execution environment
        val env = createExecutionEnvironment()

        // Create a dummy source to ensure we have operators in the streaming topology
        val dummySource = env.fromElements(1, 2, 3)
            .name("Dummy Source")
            .map { it.toString() }
            .name("Dummy Map")
            
        // Create a dummy sink to ensure the streaming topology is connected
        dummySource.print().name("Dummy Sink")

        // Create table environment
        val tableEnv = createTableEnvironment(env)

        // Create Kafka source table for flight events
        tableEnv.executeSql("""
            CREATE TABLE flight_events (
                flightId STRING,
                airline STRING,
                eventType STRING,
                `timestamp` STRING,
                latitude DOUBLE,
                longitude DOUBLE,
                delayMinutes INT,
                origin STRING,
                destination STRING,
                WATERMARK FOR `timestamp_parsed` AS `timestamp_parsed` - INTERVAL '5' SECOND,
                `timestamp_parsed` AS TO_TIMESTAMP(`timestamp`) 
            ) WITH (
                'connector' = 'kafka',
                'topic' = 'flight-events',
                'properties.bootstrap.servers' = 'localhost:29092',
                'properties.group.id' = 'flink-processor',
                'scan.startup.mode' = 'latest-offset',
                'format' = 'json'
            )
        """.trimIndent())
        
        // Create view for grid-based flight events
        tableEnv.executeSql("""
            CREATE VIEW flight_events_grid AS
            SELECT
                flightId,
                airline,
                eventType,
                `timestamp_parsed`,
                FLOOR(latitude / $gridSize) * $gridSize AS grid_lat,
                FLOOR(longitude / $gridSize) * $gridSize AS grid_lon
            FROM flight_events
        """.trimIndent())
        
        // Create JDBC sink table for flight density
        tableEnv.executeSql("""
            CREATE TABLE flight_density_sink (
                grid_lat INT,
                grid_lon INT,
                flight_count BIGINT,
                window_start TIMESTAMP(3),
                PRIMARY KEY (grid_lat, grid_lon) NOT ENFORCED
            ) WITH (
                'connector' = 'jdbc',
                'url' = 'jdbc:postgresql://localhost:5432/flightdemo',
                'table-name' = 'flight_density',
                'username' = 'postgres',
                'password' = 'postgres'
            )
        """.trimIndent())
        
        // Insert aggregated data into sink with a statement set to ensure proper execution
        val statementSet = tableEnv.createStatementSet()
        
        statementSet.addInsertSql("""
            INSERT INTO flight_density_sink
            SELECT
                CAST(grid_lat AS INT) AS grid_lat,
                CAST(grid_lon AS INT) AS grid_lon,
                COUNT(DISTINCT flightId) AS flight_count,
                COALESCE(MAX(`timestamp_parsed`), CURRENT_TIMESTAMP) AS window_start
            FROM flight_events_grid
            GROUP BY grid_lat, grid_lon
        """.trimIndent())
        
        // Execute the statement set (this will run the streaming job)
        statementSet.execute()
        logger.info { "Density aggregation job started successfully" }
    }
}
