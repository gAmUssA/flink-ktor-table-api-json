package com.demo.flight.processor.jobs

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment

/**
 * Base class for Flink jobs with common configuration.
 */
abstract class FlinkJobBase(
    val jobName: String,
    val enableWebUI: Boolean = false
) {
    protected val logger = KotlinLogging.logger {}
    
    /**
     * Configure and return a StreamExecutionEnvironment.
     */
    protected fun createExecutionEnvironment(): StreamExecutionEnvironment {
        logger.info { "Creating execution environment for $jobName" }
//        
//        val env = if (enableWebUI) {
//            // Create environment with web UI enabled
//            val config = Configuration()
//            config.setString("rest.port", "0") // Use 0 for dynamic port allocation
//            StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config)
//        } else {
            // Create standard environment
        var env = StreamExecutionEnvironment.getExecutionEnvironment()
    //}
        
        // Set parallelism
        env.parallelism = 1 // For local development, use 1
        
        return env
    }
    
    /**
     * Create a StreamTableEnvironment from a StreamExecutionEnvironment.
     */
    protected fun createTableEnvironment(env: StreamExecutionEnvironment): StreamTableEnvironment {
        // Create configuration for local execution
        val config = Configuration()
        config.setString("execution.target", "local")
        logger.info { "Creating table environment for $jobName" }
        
        val settings = EnvironmentSettings.newInstance()
            .withConfiguration(config)
            .inStreamingMode()
            .build()
        
        return StreamTableEnvironment.create(env, settings)
    }
    
    /**
     * Execute the job.
     */
    abstract fun execute()
}
