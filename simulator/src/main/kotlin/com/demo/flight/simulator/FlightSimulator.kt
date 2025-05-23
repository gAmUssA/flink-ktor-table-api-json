package com.demo.flight.simulator

import com.demo.flight.simulator.models.EventType
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
import java.time.Instant
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
    private val eventsPerSecond: Int = 10
) {
    private val logger = KotlinLogging.logger {}
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    private val kafkaProducer by lazy {
        KafkaProducer<String, String>(kafkaProperties())
    }
    
    private fun kafkaProperties(): Properties {
        return Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.RETRIES_CONFIG, 3)
        }
    }
    
    /**
     * Start the flight simulator.
     */
    fun start() {
        logger.info { "Starting Flight Simulator with $eventsPerSecond events per second" }
        
        val executor = Executors.newScheduledThreadPool(1)
        
        executor.scheduleAtFixedRate({
            try {
                generateAndSendEvent()
            } catch (e: Exception) {
                logger.error(e) { "Error generating or sending event" }
            }
        }, 0, 1000 / eventsPerSecond.toLong(), TimeUnit.MILLISECONDS)
        
        // Add shutdown hook to close resources
        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info { "Shutting down Flight Simulator" }
            executor.shutdown()
            kafkaProducer.close()
        })
    }
    
    private fun generateAndSendEvent() {
        // Generate a random flight event
        val random = Random(System.currentTimeMillis())
        val flightId = "LH${random.nextInt(100, 1000)}"
        val airline = "Lufthansa"
        val eventType = EventType.POSITION_UPDATE
        val timestamp = Instant.now()
        val latitude = 50.0379 + random.nextDouble(-0.5, 0.5)
        val longitude = 8.5622 + random.nextDouble(-0.5, 0.5)
        val delayMinutes = random.nextInt(0, 31)
        val origin = "FRA"
        val destination = "JFK"
        
        // Create the flight event
        val event = FlightEvent(
            flightId = flightId,
            airline = airline,
            eventType = eventType,
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            delayMinutes = delayMinutes,
            origin = origin,
            destination = destination
        )
        
        sendEvent(event)
    }
    
    private fun sendEvent(event: FlightEvent) {
        val json = objectMapper.writeValueAsString(event)
        val record = ProducerRecord(topic, event.flightId, json)
        
        kafkaProducer.send(record) { metadata, exception ->
            if (exception != null) {
                logger.error(exception) { "Error sending event: ${event.flightId}" }
            } else {
                logger.debug { "Event sent: ${event.flightId} to ${metadata.topic()}:${metadata.partition()} offset ${metadata.offset()}" }
            }
        }
    }
}

/**
 * Main entry point for the Flight Simulator application.
 */
fun main(args: Array<String>) {
    val bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:29092"
    val topic = System.getenv("KAFKA_TOPIC") ?: "flight-events"
    val eventsPerSecond = System.getenv("EVENTS_PER_SECOND")?.toIntOrNull() ?: 10
    
    FlightSimulator(bootstrapServers, topic, eventsPerSecond).start()
}
