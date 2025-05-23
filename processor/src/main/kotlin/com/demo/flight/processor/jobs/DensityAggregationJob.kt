package com.demo.flight.processor.jobs

import org.apache.flink.table.api.bridge.java.StreamTableEnvironment

/**
 * Flink job for aggregating flight density by geographic grid.
 * Uses the Table API with SQL to process flight events and calculate density.
 */
class DensityAggregationJob : FlinkJobBase("density-aggregation-job") {
    
    // Grid size in degrees (approximately 50km at the equator)
    private val gridSize = 0.5
    
    override fun execute() {
        logger.info { "Starting $jobName" }
        
        // Create execution environment
        val env = createExecutionEnvironment()
        
        // Create table environment
        val tableEnv = createTableEnvironment(env)
        
        // Create the Kafka source table
        logger.info { "Creating Kafka source table for flight events" }
        createKafkaSourceTable(tableEnv)
        
        // Create the PostgreSQL sink table
        logger.info { "Creating PostgreSQL sink table for flight density" }
        createJdbcSinkTable(tableEnv)
        
        // Process the flight events to calculate density using SQL
        logger.info { "Processing flight events to calculate density using SQL" }
        calculateFlightDensityWithSql(tableEnv)
        
        // Execute the job
        logger.info { "Executing $jobName" }
        env.execute(jobName)
        
        logger.info { "$jobName submitted" }
    }
    
    /**
     * Create the Kafka source table for flight events.
     */
    private fun createKafkaSourceTable(tableEnv: StreamTableEnvironment) {
        // Create a table from Kafka with JSON format
        tableEnv.executeSql("""
            CREATE TABLE flight_events (
                flightId STRING,
                airline STRING,
                eventType STRING,
                timestamp TIMESTAMP(3),
                latitude DOUBLE,
                longitude DOUBLE,
                delayMinutes INT,
                origin STRING,
                destination STRING,
                WATERMARK FOR timestamp AS timestamp - INTERVAL '5' SECOND
            ) WITH (
                'connector' = 'kafka',
                'topic' = 'flight-events',
                'properties.bootstrap.servers' = 'localhost:29092',
                'properties.group.id' = 'flink-processor',
                'scan.startup.mode' = 'latest-offset',
                'format' = 'json'
            )
        """.trimIndent())
        
        // Create a view with grid coordinates for easier querying
        tableEnv.executeSql("""
            CREATE VIEW flight_events_grid AS
            SELECT
                flightId,
                airline,
                eventType,
                timestamp,
                FLOOR(latitude / $gridSize) * $gridSize AS grid_lat,
                FLOOR(longitude / $gridSize) * $gridSize AS grid_lon
            FROM flight_events
        """.trimIndent())
    }
    
    /**
     * Create the JDBC sink table for flight density.
     */
    private fun createJdbcSinkTable(tableEnv: StreamTableEnvironment) {
        // Create a JDBC sink table for PostgreSQL
        tableEnv.executeSql("""
            CREATE TABLE flight_density_sink (
                grid_lat DOUBLE,
                grid_lon DOUBLE,
                flight_count INT,
                event_timestamp TIMESTAMP(3),
                PRIMARY KEY (grid_lat, grid_lon) NOT ENFORCED
            ) WITH (
                'connector' = 'jdbc',
                'url' = 'jdbc:postgresql://localhost:5432/flightdb',
                'table-name' = 'flight_density',
                'username' = 'postgres',
                'password' = 'postgres'
            )
        """.trimIndent())
    }
    
    /**
     * Calculate flight density by geographic grid using SQL.
     */
    private fun calculateFlightDensityWithSql(tableEnv: StreamTableEnvironment) {
        // Execute SQL to calculate flight density and insert into sink
        tableEnv.executeSql("""
            INSERT INTO flight_density_sink
            SELECT
                grid_lat,
                grid_lon,
                COUNT(*) AS flight_count,
                MAX(timestamp) AS event_timestamp
            FROM flight_events_grid
            GROUP BY grid_lat, grid_lon
        """.trimIndent())
    }
}
