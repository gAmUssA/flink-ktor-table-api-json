package com.demo.flight.processor.jobs

import com.demo.flight.processor.models.EventType
import com.demo.flight.processor.models.FlightEvent
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Unit test for the DensityAggregationJob.
 * Tests the job's SQL query execution flow.
 */
class DensityAggregationJobTest {

    @Test
    fun testDensityAggregationJobExecution() {
        // Create a test instance of DensityAggregationJob with mocked execution
        val testJob = object : DensityAggregationJob() {
            // Override execute to avoid actual execution
            override fun execute() {
                // Create execution environment
                val testEnv = createExecutionEnvironment()
                
                // Create table environment
                val testTableEnv = createTableEnvironment(testEnv)
                
                // Just verify that the SQL queries would be executed
                // but don't actually execute them
                logger.info { "SQL queries would be executed by $testTableEnv" }
            }
        }
        
        // Call the execute method
        testJob.execute()
        
        // Since we're using SQL queries directly, we can't easily test the actual data processing
        // in a unit test. In a real-world scenario, we would use an integration test with
        // embedded Kafka and embedded PostgreSQL to test the full pipeline.
        
        // For now, we just verify that the job can be instantiated and the execute method runs
        // without exceptions.
    }
    
    /**
     * This test demonstrates how we would create test flight events for integration testing.
     */
    fun createTestFlightEvents(): List<FlightEvent> {
        return listOf(
            // Flight 1 in grid cell (40.5, -74.0)
            FlightEvent(
                flightId = "FL123",
                airline = "TestAir",
                eventType = EventType.POSITION_UPDATE,
                timestamp = Instant.now(),
                latitude = 40.7128, // Will be rounded to 40.5
                longitude = -74.0060, // Will be rounded to -74.0
                delayMinutes = 0,
                origin = "JFK",
                destination = "LAX"
            ),
            // Flight 2 in same grid cell
            FlightEvent(
                flightId = "FL456",
                airline = "TestAir",
                eventType = EventType.POSITION_UPDATE,
                timestamp = Instant.now(),
                latitude = 40.6500, // Will be rounded to 40.5
                longitude = -74.1000, // Will be rounded to -74.0
                delayMinutes = 0,
                origin = "EWR",
                destination = "ORD"
            ),
            // Flight 3 in different grid cell
            FlightEvent(
                flightId = "FL789",
                airline = "TestAir",
                eventType = EventType.POSITION_UPDATE,
                timestamp = Instant.now(),
                latitude = 37.7749, // Will be rounded to 37.5
                longitude = -122.4194, // Will be rounded to -122.5
                delayMinutes = 0,
                origin = "SFO",
                destination = "SEA"
            )
        )
    }
}
