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
                DensityAggregationJob().execute()
            }
            "all" -> {
                logger.info { "Running all jobs" }
                // Note: In a production environment, these would typically be submitted to a Flink cluster
                // rather than executed sequentially in the same JVM
                DelayDetectionJob().execute()
                DensityAggregationJob().execute()
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
