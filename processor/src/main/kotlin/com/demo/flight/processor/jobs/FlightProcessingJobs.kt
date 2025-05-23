package com.demo.flight.processor.jobs

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Main entry point for the Flight Processing Jobs.
 * This class is responsible for creating and executing the Flink jobs.
 */
object FlightProcessingJobs {
    private val logger = KotlinLogging.logger {}
    
    /**
     * Main method to run the Flink jobs.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info { "Starting Flight Processing Jobs" }
        
        val jobType = args.firstOrNull() ?: "all"
        
        when (jobType.lowercase()) {
            "delay" -> {
                logger.info { "Running Delay Detection Job" }
                DelayDetectionJob().execute()
            }
            "density" -> {
                logger.info { "Running Density Aggregation Job" }
                // Density job will be implemented in Phase 3
                logger.info { "Density Aggregation Job not yet implemented" }
            }
            "all" -> {
                logger.info { "Running all jobs" }
                DelayDetectionJob().execute()
                // Density job will be implemented in Phase 3
                logger.info { "Density Aggregation Job not yet implemented" }
            }
            else -> {
                logger.error { "Unknown job type: $jobType" }
                logger.info { "Available job types: delay, density, all" }
                System.exit(1)
            }
        }
        
        logger.info { "All jobs submitted" }
    }
}
