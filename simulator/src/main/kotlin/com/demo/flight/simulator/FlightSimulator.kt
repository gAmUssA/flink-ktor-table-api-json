package com.demo.flight.simulator

import com.demo.flight.simulator.models.FlightEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Main class for the Flight Simulator application.
 * Generates flight events and sends them to Kafka.
 */
class FlightSimulator(
    private val bootstrapServers: String = "localhost:29092",
    private val topic: String = "flight-events",
    private val eventsPerSecond: Int = 10,
    private val configPath: String? = null
) {
    private val logger = KotlinLogging.logger {}
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    // Event generator for creating realistic flight events
    private val eventGenerator = EventGenerator(
        random = Random(System.currentTimeMillis()),
        configPath = configPath
    )
    
    // Metrics for monitoring
    private var eventsSent = 0
    private var errorCount = 0
    
    private val kafkaProducer by lazy {
        KafkaProducer<String, String>(kafkaProperties())
    }
    
    private fun kafkaProperties(): Properties {
        val props = Properties()
        
        // Load from config file if provided
        configPath?.let { path ->
            val configFile = File(path)
            if (configFile.exists()) {
                logger.info { "Loading Kafka configuration from $path" }
                FileInputStream(configFile).use { props.load(it) }
            } else {
                logger.warn { "Configuration file not found: $path" }
            }
        }
        
        // Set or override essential properties
        props.apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.RETRIES_CONFIG, 3)
            put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000)
            put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1)
            put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true)
            
            // Performance tuning
            put(ProducerConfig.BATCH_SIZE_CONFIG, 16384)
            put(ProducerConfig.LINGER_MS_CONFIG, 5)
            put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432) // 32MB
            put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy")
        }
        
        return props
    }
    
    /**
     * Start the flight simulator.
     */
    fun start() {
        logger.info { "Starting Flight Simulator with $eventsPerSecond events per second" }
        
        val executor = Executors.newScheduledThreadPool(2)
        
        // Main event generation task
        executor.scheduleAtFixedRate({
            try {
                generateAndSendEvent()
            } catch (e: Exception) {
                logger.error(e) { "Error generating or sending event" }
                errorCount++
            }
        }, 0, 1000 / eventsPerSecond.toLong(), TimeUnit.MILLISECONDS)
        
        // Metrics reporting task
        executor.scheduleAtFixedRate({
            logger.info { "Simulator stats: $eventsSent events sent, $errorCount errors" }
        }, 10, 10, TimeUnit.SECONDS)
        
        // Add shutdown hook to close resources
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info { "Shutting down Flight Simulator" }
            executor.shutdown()
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executor.shutdownNow()
            }
            kafkaProducer.close()
            logger.info { "Flight Simulator shutdown complete. Total events sent: $eventsSent" }
        })
        
        logger.info { "Flight Simulator started successfully" }
    }
    
    private fun generateAndSendEvent() {
        // Use the event generator to create a realistic flight event
        val event = eventGenerator.generateEvent()
        sendEvent(event)
    }
    
    private fun sendEvent(event: FlightEvent) {
        val json = objectMapper.writeValueAsString(event)
        val record = ProducerRecord(topic, event.flightId, json)
        
        kafkaProducer.send(record) { metadata, exception ->
            if (exception != null) {
                logger.error(exception) { "Error sending event: ${event.flightId}" }
                errorCount++
            } else {
                logger.debug { "Event sent: ${event.flightId} to ${metadata.topic()}:${metadata.partition()} offset ${metadata.offset()}" }
                eventsSent++
            }
        }
    }
}

/**
 * Main entry point for the Flight Simulator application.
 */
fun main(args: Array<String>) {
    val logger = KotlinLogging.logger {}
    
    // Parse command line arguments
    var bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:29092"
    var topic = System.getenv("KAFKA_TOPIC") ?: "flight-events"
    var eventsPerSecond = System.getenv("EVENTS_PER_SECOND")?.toIntOrNull() ?: 10
    var configPath: String? = System.getenv("CONFIG_PATH")
    
    // Override with command line arguments if provided
    args.forEachIndexed { index, arg ->
        when (arg) {
            "--bootstrap-servers", "-b" -> if (index + 1 < args.size) bootstrapServers = args[index + 1]
            "--topic", "-t" -> if (index + 1 < args.size) topic = args[index + 1]
            "--events-per-second", "-e" -> if (index + 1 < args.size) eventsPerSecond = args[index + 1].toIntOrNull() ?: eventsPerSecond
            "--config", "-c" -> if (index + 1 < args.size) configPath = args[index + 1]
            "--help", "-h" -> {
                println("""
                    Flight Simulator - Generates realistic flight events and sends them to Kafka
                    
                    Usage: java -jar flight-simulator.jar [options]
                    
                    Options:
                      -b, --bootstrap-servers <servers>  Kafka bootstrap servers (default: localhost:29092)
                      -t, --topic <topic>                Kafka topic (default: flight-events)
                      -e, --events-per-second <count>    Number of events to generate per second (default: 10)
                      -c, --config <path>                Path to configuration file
                      -h, --help                         Show this help message
                    
                    Environment variables:
                      KAFKA_BOOTSTRAP_SERVERS            Same as --bootstrap-servers
                      KAFKA_TOPIC                        Same as --topic
                      EVENTS_PER_SECOND                  Same as --events-per-second
                      CONFIG_PATH                        Same as --config
                """.trimIndent())
                return
            }
        }
    }
    
    logger.info { "Starting Flight Simulator with the following configuration:" }
    logger.info { "Bootstrap Servers: $bootstrapServers" }
    logger.info { "Topic: $topic" }
    logger.info { "Events Per Second: $eventsPerSecond" }
    logger.info { "Config Path: ${configPath ?: "Not specified"}" }
    
    FlightSimulator(bootstrapServers, topic, eventsPerSecond, configPath).start()
}
