package com.demo.flight.api.services

import com.demo.flight.api.models.FlightEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service for handling flight events from Kafka and broadcasting to WebSocket clients.
 */
class FlightEventService(
    private val bootstrapServers: String = "localhost:29092",
    private val topic: String = "flight-events",
    private val groupId: String = "api-server"
) {
    private val logger = KotlinLogging.logger {}
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val running = AtomicBoolean(false)
    
    // Shared flow for broadcasting events to all WebSocket clients
    private val _flightEvents = MutableSharedFlow<FlightEvent>(replay = 0, extraBufferCapacity = 100)
    val flightEvents: SharedFlow<FlightEvent> = _flightEvents.asSharedFlow()
    
    // Jackson object mapper for JSON serialization/deserialization
    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    
    /**
     * Start consuming flight events from Kafka and broadcasting to WebSocket clients.
     */
    fun start() {
        if (running.compareAndSet(false, true)) {
            logger.info { "Starting FlightEventService with Kafka bootstrap servers: $bootstrapServers" }
            
            serviceScope.launch {
                try {
                    consumeFlightEvents()
                } catch (e: Exception) {
                    logger.error(e) { "Error consuming flight events" }
                    running.set(false)
                }
            }
        }
    }
    
    /**
     * Stop consuming flight events from Kafka.
     */
    fun stop() {
        if (running.compareAndSet(true, false)) {
            logger.info { "Stopping FlightEventService" }
        }
    }
    
    /**
     * Consume flight events from Kafka and broadcast to WebSocket clients.
     */
    private suspend fun consumeFlightEvents() {
        val consumer = createKafkaConsumer()
        
        try {
            consumer.subscribe(listOf(topic))
            logger.info { "Subscribed to Kafka topic: $topic" }
            
            while (running.get()) {
                val records = consumer.poll(Duration.ofMillis(100))
                
                for (record in records) {
                    try {
                        val flightEvent = objectMapper.readValue(record.value(), FlightEvent::class.java)
                        logger.debug { "Received flight event: ${flightEvent.flightId}" }
                        
                        // Broadcast the event to all WebSocket clients
                        _flightEvents.emit(flightEvent)
                    } catch (e: Exception) {
                        logger.error(e) { "Error processing flight event: ${record.value()}" }
                    }
                }
            }
        } finally {
            consumer.close()
            logger.info { "Kafka consumer closed" }
        }
    }
    
    /**
     * Create a Kafka consumer with the appropriate configuration.
     */
    private fun createKafkaConsumer(): KafkaConsumer<String, String> {
        val props = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
            put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")
        }
        
        return KafkaConsumer(props)
    }
}
