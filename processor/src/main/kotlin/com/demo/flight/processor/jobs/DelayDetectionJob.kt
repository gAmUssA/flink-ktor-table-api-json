package com.demo.flight.processor.jobs

import com.demo.flight.processor.models.EventType
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import java.time.Instant

/**
 * Flink job for detecting delayed flights.
 * Uses the Table API with SQL to process flight events and detect delays.
 */
open class DelayDetectionJob : FlinkJobBase("delay-detection-job", true) {
    
    override fun execute() {
        logger.info { "Starting $jobName" }
        
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
                destination STRING
            ) WITH (
                'connector' = 'kafka',
                'topic' = 'flight-events',
                'properties.bootstrap.servers' = 'localhost:29092',
                'properties.group.id' = 'flink-processor',
                'scan.startup.mode' = 'latest-offset',
                'format' = 'json'
            )
        """.trimIndent())
        
        // Create PostgreSQL sink table for delayed flights
        tableEnv.executeSql("""
            CREATE TABLE delayed_flights_sink (
                flight_id STRING,
                airline STRING,
                delay_minutes INT,
                origin STRING,
                destination STRING,
                `timestamp` TIMESTAMP(3),
                PRIMARY KEY (flight_id) NOT ENFORCED
            ) WITH (
                'connector' = 'jdbc',
                'url' = 'jdbc:postgresql://localhost:5432/flightdemo',
                'table-name' = 'delayed_flights',
                'username' = 'postgres',
                'password' = 'postgres'
            )
        """.trimIndent())
        
        // Create a statement set for executing SQL operations
        val statementSet = tableEnv.createStatementSet()
        
        // Add the SQL insert statement to the statement set
        statementSet.addInsertSql("""
            INSERT INTO delayed_flights_sink
            SELECT 
                flightId AS flight_id,
                airline,
                delayMinutes AS delay_minutes,
                origin,
                destination,
                CAST(REPLACE(REPLACE(`timestamp`, 'T', ' '), 'Z', '') AS TIMESTAMP) AS `timestamp`
            FROM flight_events
            WHERE delayMinutes > 0
        """.trimIndent())
        
        // Execute the statement set
        statementSet.execute()
        
        logger.info { "Delay detection job started successfully" }
    }
}
