# Flight Control Center Demo Application (Simplified)
## Product Requirements Document (PRD)

**Version**: 2.0 (Simplified)  
**Date**: May 23, 2025  
**Target**: Conference Demo for "From REST to Streams" Talk  
**Duration**: 45-minute presentation with 10-minute live demo  

---

## Executive Summary

Build a simplified real-time flight operations dashboard that demonstrates the core concepts of stream processing vs. traditional REST approaches. The application showcases Apache Flink's stream processing with Kafka using simple JSON messages, integrated with Ktor to create an accessible demo that conference attendees can immediately understand and replicate.

**Key Simplification**: No schema registry, no Avro complexity - just pure Kafka + JSON + Flink + Ktor + PostgreSQL.

---

## Problem Statement

**The Pain Point**: Traditional flight information systems rely on batch processing and manual refresh patterns, leading to stale data and poor user experience.

**The Demo Challenge**: Most streaming demos are overly complex with enterprise features that obscure the core concepts. This simplified version focuses on the fundamental value proposition without operational complexity.

---

## Success Criteria

### Demo Success Metrics
- [ ] Audience can clearly see REST vs Stream differences
- [ ] Live data flows with visible updates in under 5 seconds
- [ ] Simple architecture that developers can replicate immediately
- [ ] Zero configuration complexity - just docker-compose up

### Technical Success Metrics
- [ ] Handle 100+ flight events per second (realistic demo load)
- [ ] Sub-second response times for all endpoints
- [ ] Simple JSON throughout - no serialization complexity
- [ ] PostgreSQL materialized views update in real-time

---

## Simplified Architecture

### System Overview
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Flight Generator│───▶│   Kafka (JSON)   │───▶│  Flink Jobs     │
│    (Simulator)  │    │  Simple Topics   │    │ (Table API)     │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Web Dashboard  │◀───│   Ktor Service   │◀───│   PostgreSQL    │
│   (Frontend)    │    │ (REST/WebSocket) │    │ (Materialized)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Technology Stack (Simplified)

#### Core Components
- **Kafka 3.5+**: Simple JSON message broker
- **Apache Flink 2.0**: Stream processing with Table API
- **PostgreSQL 15**: Materialized state storage
- **Ktor 2.3+**: Web framework and REST API
- **Kotlin 1.9+**: Primary programming language

#### Removed Complexity
- ❌ Schema Registry
- ❌ Avro serialization
- ❌ Confluent Platform features
- ❌ Complex state stores
- ❌ Advanced windowing strategies

---

## Core Features (Simplified)

### 1. Flight Event Generation
**Simple JSON Event Schema:**
```json
{
  "flightId": "LH441",
  "airline": "Lufthansa",
  "eventType": "POSITION_UPDATE",
  "timestamp": "2025-05-23T14:30:00Z",
  "latitude": 50.0379,
  "longitude": 8.5622,
  "delayMinutes": 0,
  "origin": "FRA",
  "destination": "JFK"
}
```

### 2. Delay Detection (Stateless Processing)
**User Story**: *As an operations manager, I want immediate alerts when flights are delayed.*

**Fluent API Implementation:**
```kotlin
// Simple, readable Kotlin-style processing
val flightEvents = tableEnv.from("flight_events")

val delayedFlights = flightEvents
    .filter($("delayMinutes").isGreater(15))
    .select(
        $("flightId"),
        $("airline"), 
        $("delayMinutes"),
        $("origin"),
        $("destination"),
        $("eventTime")
    )

// Insert into PostgreSQL
delayedFlights.executeInsert("delayed_flights_sink")
```

### 3. Flight Density Aggregation (Stateful Processing)
**User Story**: *As an air traffic controller, I want to see flight counts by region every minute.*

**Fluent API with Windowing:**
```kotlin
val flightEvents = tableEnv.from("flight_events")

val flightDensity = flightEvents
    .filter($("eventType").isEqual("POSITION_UPDATE"))
    .window(Tumble.over(lit(1).minutes()).on($("eventTime")).as("w"))
    .groupBy($("w"), $("latitude").floor().div(10).times(10), $("longitude").floor().div(10).times(10))
    .select(
        $("latitude").floor().div(10).times(10).as("grid_lat"),
        $("longitude").floor().div(10).times(10).as("grid_lon"),
        $("flightId").count().as("flight_count"),
        $("w").start().as("window_start")
    )

// Stream to PostgreSQL
flightDensity.executeInsert("flight_density_sink")
```

### 4. PostgreSQL Materialized Views
**Simple State Storage with Upsert:**
```kotlin
// Configure PostgreSQL sink with upsert capability
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
        'table-name' = 'current_flight_density',
        'sink.buffer-flush.max-rows' = '1',
        'sink.buffer-flush.interval' = '0'
    )
""")
```

### 5. Simple REST API
**Basic Endpoints:**
```kotlin
// Ktor routing with simple JSON responses
fun Application.configureRouting() {
    routing {
        get("/api/flights/density") {
            val density = database.getCurrentDensity()
            call.respond(density)
        }
        
        get("/api/flights/delayed") {
            val delayed = database.getDelayedFlights()
            call.respond(delayed)
        }
        
        webSocket("/api/flights/live") {
            // Simple Kafka consumer pushing JSON updates
            kafkaConsumer.subscribe(listOf("flight-updates"))
            while (true) {
                val records = kafkaConsumer.poll(Duration.ofMillis(100))
                records.forEach { record ->
                    send(Frame.Text(record.value())) // Raw JSON
                }
            }
        }
    }
}
```

---

## Simplified Implementation

### 1. Flight Event Generator
```kotlin
// Simple flight simulator
data class FlightEvent(
    val flightId: String,
    val airline: String,
    val eventType: String,
    val timestamp: String,
    val latitude: Double,
    val longitude: Double,
    val delayMinutes: Int,
    val origin: String,
    val destination: String
)

class FlightEventGenerator {
    private val airlines = listOf("LH", "UA", "BA", "AF", "DL")
    private val airports = listOf("FRA", "JFK", "LHR", "CDG", "LAX")
    
    fun generateEvent(): FlightEvent {
        return FlightEvent(
            flightId = "${airlines.random()}${Random.nextInt(100, 999)}",
            airline = airlines.random(),
            eventType = listOf("POSITION_UPDATE", "DELAY", "TAKEOFF", "LANDING").random(),
            timestamp = Instant.now().toString(),
            latitude = Random.nextDouble(-90.0, 90.0),
            longitude = Random.nextDouble(-180.0, 180.0),
            delayMinutes = if (Random.nextFloat() < 0.2) Random.nextInt(15, 120) else 0,
            origin = airports.random(),
            destination = airports.random()
        )
    }
}
```

### 2. Flink Processing (Fluent API)
```kotlin
// Simple, intuitive Flink Table API processing
object FlightProcessingJob {
    fun main(args: Array<String>) {
        val env = StreamExecutionEnvironment.getExecutionEnvironment()
        val tableEnv = StreamTableEnvironment.create(env)
        
        // Define source table (simple JSON from Kafka)
        tableEnv.executeSql("""
            CREATE TABLE flight_events (
                flightId STRING,
                airline STRING,
                eventType STRING,
                eventTime TIMESTAMP(3),
                latitude DOUBLE,
                longitude DOUBLE,
                delayMinutes INT,
                origin STRING,
                destination STRING,
                WATERMARK FOR eventTime AS eventTime - INTERVAL '5' SECOND
            ) WITH (
                'connector' = 'kafka',
                'topic' = 'flight-events',
                'properties.bootstrap.servers' = 'localhost:9092',
                'format' = 'json'
            )
        """)
        
        // Get source table reference
        val flightEvents = tableEnv.from("flight_events")
        
        // Delay detection with fluent API - feels like Kotlin collections!
        val delayedFlights = flightEvents
            .filter($("delayMinutes").isGreater(15))
            .select(
                $("flightId"),
                $("airline"),
                $("delayMinutes"), 
                $("origin"),
                $("destination"),
                $("eventTime")
            )
        
        // Flight density with fluent windowing
        val flightDensity = flightEvents
            .filter($("eventType").isEqual("POSITION_UPDATE"))
            .window(Tumble.over(lit(1).minutes()).on($("eventTime")).as("w"))
            .groupBy(
                $("w"),
                $("latitude").floor().div(10).times(10).as("grid_lat"),
                $("longitude").floor().div(10).times(10).as("grid_lon")
            )
            .select(
                $("grid_lat"),
                $("grid_lon"),
                $("flightId").count().as("flight_count"),
                $("w").start().as("window_start")
            )
        
        // Define PostgreSQL sinks
        createPostgreSQLSinks(tableEnv)
        
        // Execute - fluent API makes it clear what happens
        delayedFlights.executeInsert("delayed_flights_sink")
        flightDensity.executeInsert("flight_density_sink")
        
        env.execute("Simple Flight Processing")
    }
    
    private fun createPostgreSQLSinks(tableEnv: StreamTableEnvironment) {
        // Delayed flights sink - simple upsert table
        tableEnv.executeSql("""
            CREATE TABLE delayed_flights_sink (
                flightId STRING,
                airline STRING,
                delayMinutes INT,
                origin STRING,
                destination STRING,
                eventTime TIMESTAMP(3),
                PRIMARY KEY (flightId) NOT ENFORCED
            ) WITH (
                'connector' = 'jdbc',
                'url' = 'jdbc:postgresql://localhost:5432/flightdemo',
                'table-name' = 'current_delayed_flights'
            )
        """)
        
        // Flight density sink - aggregated data
        tableEnv.executeSql("""
            CREATE TABLE flight_density_sink (
                grid_lat DOUBLE,
                grid_lon DOUBLE,
                flight_count BIGINT,
                window_start TIMESTAMP(3),
                PRIMARY KEY (grid_lat, grid_lon) NOT ENFORCED
            ) WITH (
                'connector' = 'jdbc',
                'url' = 'jdbc:postgresql://localhost:5432/flightdemo',
                'table-name' = 'current_flight_density'
            )
        """)
    }
}
```

### 3. Ktor Service (Minimal)
```kotlin
// Simple Ktor application
fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            jackson()
        }
        install(WebSockets)
        
        routing {
            static("/") {
                resources("static")
            }
            
            get("/api/density") {
                val result = database.query("""
                    SELECT grid_lat, grid_lon, flight_count, last_updated 
                    FROM current_flight_density 
                    ORDER BY flight_count DESC
                """)
                call.respond(result)
            }
            
            get("/api/delayed") {
                val result = database.query("""
                    SELECT * FROM current_delayed_flights 
                    WHERE delay_minutes > 15
                    ORDER BY delay_minutes DESC
                """)
                call.respond(result)
            }
            
            webSocket("/live") {
                val consumer = KafkaConsumer<String, String>(kafkaProps)
                consumer.subscribe(listOf("flight-events"))
                
                while (true) {
                    val records = consumer.poll(Duration.ofMillis(100))
                    records.forEach { record ->
                        send(Frame.Text(record.value()))
                    }
                }
            }
        }
    }.start(wait = true)
}
```

## Why Fluent API? (For Kotlin Developers)

### **Feels Like Kotlin Collections**
```kotlin
// This feels familiar to Kotlin developers:
val delayedFlights = flightEvents
    .filter($("delayMinutes").isGreater(15))
    .select($("flightId"), $("airline"), $("delayMinutes"))

// Just like:
val delayedItems = items
    .filter { it.delay > 15 }
    .map { Triple(it.id, it.name, it.delay) }
```

### **Type Safety & IDE Support**
- **IntelliJ Integration**: Full autocomplete and refactoring support
- **Compile-time Validation**: Catch errors before runtime
- **Method Chaining**: Natural, readable data transformations

### **Visual Pipeline Construction**
```kotlin
// The data flow is obvious by reading the code:
sourceTable
    .filter(conditions)          // 1. Filter events
    .window(timeWindow)          // 2. Create time windows  
    .groupBy(keys)               // 3. Group by region
    .select(aggregations)        // 4. Calculate counts
    .executeInsert(sinkTable)    // 5. Save to database
```

### **Demo Advantage**
- **Live Coding Friendly**: Easy to modify during presentation
- **Audience Comprehension**: Looks like code they already write
- **Interactive Development**: Can build queries step by step
- **Debugging**: Easy to add `.print()` calls for intermediate results

---

## Demo Flow with Fluent API

### **Live Coding Demonstration**
```kotlin
// Start simple - show the source
val flights = tableEnv.from("flight_events")
flights.limit(5).execute().print() // Show sample data

// Add delay detection step by step
val delayed = flights.filter($("delayMinutes").isGreater(15))
delayed.limit(5).execute().print() // Show filtered results

// Add windowing for aggregation
val density = flights
    .filter($("eventType").isEqual("POSITION_UPDATE"))
    .window(Tumble.over(lit(1).minutes()).on($("eventTime")).as("w"))
    
// Build aggregation interactively
val regionCounts = density
    .groupBy($("w"), gridLat, gridLon)
    .select(gridLat, gridLon, $("flightId").count().as("count"))

// Finally - connect to output
regionCounts.executeInsert("flight_density_sink")
```

### **Key Demo Moments**
1. **"Look, it's just Kotlin!"** - Show filter/map similarity
2. **"Build it step by step"** - Add operations incrementally  
3. **"See intermediate results"** - Use `.execute().print()` 
4. **"Connect to real data"** - Stream to PostgreSQL
5. **"Query anytime"** - Show REST API reading results

### Scenario 1: Basic Flow (2 minutes)
1. Show flight events generating in simple JSON format
2. Watch Flink process events with SQL queries visible
3. See PostgreSQL tables updating in real-time
4. Query REST endpoints for current state

### Scenario 2: Delay Alerts (2 minutes)
1. Trigger delayed flight events
2. Show immediate detection in Flink
3. Watch alerts appear in dashboard
4. Query delayed flights via REST

### Scenario 3: Density Aggregation (2 minutes)
1. Generate position updates across regions
2. Show windowed aggregation creating density data
3. Watch grid counts update every minute
4. Display on simple world map

### Scenario 4: REST vs Stream Comparison (4 minutes)
1. Show traditional database query approach
2. Compare with live streaming updates
3. Demonstrate freshness difference
4. Show both working together harmoniously

---

## Simplified Docker Setup

### docker-compose.yml
```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on: [zookeeper]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: flightdemo
      POSTGRES_USER: demo
      POSTGRES_PASSWORD: demo
    ports:
      - "5432:5432"

  flink-jobmanager:
    image: flink:2.0-scala_2.12
    command: jobmanager
    environment:
      - FLINK_PROPERTIES=jobmanager.rpc.address: jobmanager

  flink-taskmanager:
    image: flink:2.0-scala_2.12
    depends_on: [flink-jobmanager]
    command: taskmanager
    environment:
      - FLINK_PROPERTIES=jobmanager.rpc.address: jobmanager
```

### Enhanced Demo Orchestration Makefile
```makefile
# Makefile for Flight Demo - Flink Use Cases
.PHONY: help setup demo-* clean monitor logs

# Colors for output
RED=\033[0;31m
GREEN=\033[0;32m
YELLOW=\033[1;33m
BLUE=\033[0;34m
NC=\033[0m # No Color

help: ## Show this help message
	@echo '${BLUE}Flight Demo - Available Commands:${NC}'
	@echo ''
	@echo '${YELLOW}Setup Commands:${NC}'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  ${GREEN}%-20s${NC} %s\n", $1, $2}' $(MAKEFILE_LIST) | grep -E "(setup|clean)"
	@echo ''
	@echo '${YELLOW}Demo Scenarios:${NC}'
	@awk 'BEGIN {FS = ":.*?## "} /^demo-.*:.*?## / {printf "  ${GREEN}%-20s${NC} %s\n", $1, $2}' $(MAKEFILE_LIST)
	@echo ''
	@echo '${YELLOW}Monitoring:${NC}'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  ${GREEN}%-20s${NC} %s\n", $1, $2}' $(MAKEFILE_LIST) | grep -E "(monitor|logs)"

## Setup Commands
setup: ## Setup complete demo environment
	@echo "${BLUE}🚀 Setting up flight demo environment...${NC}"
	docker-compose -f docker/docker-compose.yml up -d
	@echo "${YELLOW}⏳ Waiting for services to be ready...${NC}"
	./scripts/wait-for-services.sh
	@echo "${YELLOW}📊 Creating Kafka topics...${NC}"
	./scripts/create-topics.sh
	@echo "${YELLOW}🗄️ Initializing PostgreSQL database...${NC}"
	./scripts/init-database.sh
	@echo "${GREEN}✅ Environment ready! Use 'make demo-interactive' to start${NC}"

clean: ## Clean up all demo resources
	@echo "${RED}🧹 Cleaning up demo environment...${NC}"
	docker-compose -f docker/docker-compose.yml down -v
	./gradlew clean
	pkill -f "FlightEventGenerator" || true
	pkill -f "FlightProcessingJob" || true
	pkill -f "KtorService" || true
	@echo "${GREEN}✅ Cleanup complete${NC}"

## Demo Scenario 1: Interactive Query Building
demo-interactive: ## Live-code delay detection step by step
	@echo "${BLUE}🎭 Starting Interactive Demo - Delay Detection${NC}"
	@echo "${YELLOW}Step 1: Starting flight event generator...${NC}"
	./gradlew :simulator:run &
	@echo "${YELLOW}Step 2: Ready for live coding demo!${NC}"
	@echo "${GREEN}▶️  Open your IDE and start with: tableEnv.from('flight_events')${NC}"
	@echo "${BLUE}📋 Commands available:${NC}"
	@echo "   make demo-show-events     # Show live events"
	@echo "   make demo-delay-filter    # Apply delay filter" 
	@echo "   make demo-save-to-db      # Save to PostgreSQL"
	@echo "   make demo-query-rest      # Query via REST API"

demo-show-events: ## Show live flight events (Step 1 of interactive demo)
	@echo "${BLUE}📡 Showing live flight events...${NC}"
	./scripts/show-kafka-events.sh flight-events

demo-delay-filter: ## Start delay detection job (Step 2 of interactive demo)
	@echo "${BLUE}🔍 Starting delay detection with Flink Fluent API...${NC}"
	./gradlew :flink-jobs:runDelayDetection &
	@echo "${GREEN}▶️  Watch delayed flights appear in console${NC}"
	sleep 3
	./scripts/show-flink-output.sh DelayDetectionJob

demo-save-to-db: ## Save delay results to PostgreSQL (Step 3 of interactive demo)
	@echo "${BLUE}💾 Connecting delay detection to PostgreSQL...${NC}"
	./gradlew :flink-jobs:runDelayToDB &
	sleep 2
	@echo "${GREEN}▶️  Check database updates:${NC}"
	./scripts/show-db-updates.sh current_delayed_flights

demo-query-rest: ## Start REST API and show results (Step 4 of interactive demo)
	@echo "${BLUE}🌐 Starting Ktor REST API...${NC}"
	./gradlew :ktor-service:run &
	sleep 3
	@echo "${GREEN}▶️  Testing REST endpoint:${NC}"
	curl -s http://localhost:8080/api/delayed | jq '.'

## Demo Scenario 2: Windowed Aggregation  
demo-aggregation: ## Demonstrate flight density aggregation
	@echo "${BLUE}🗺️ Starting Windowed Aggregation Demo${NC}"
	@echo "${YELLOW}Step 1: Generating position updates...${NC}"
	./gradlew :simulator:runPositionUpdates &
	@echo "${YELLOW}Step 2: Starting density aggregation job...${NC}"
	./gradlew :flink-jobs:runDensityAggregation &
	@echo "${GREEN}▶️  Available commands:${NC}"
	@echo "   make demo-show-windows    # Show windowing in action"
	@echo "   make demo-show-density    # Show density calculations"
	@echo "   make demo-density-map     # Open density heat map"

demo-show-windows: ## Show tumbling window operations
	@echo "${BLUE}⏰ Showing 1-minute tumbling windows...${NC}"
	./scripts/show-flink-windows.sh DensityAggregationJob

demo-show-density: ## Show density calculation results
	@echo "${BLUE}📊 Current flight density by region:${NC}"
	./scripts/show-db-updates.sh current_flight_density

demo-density-map: ## Open density visualization in browser
	@echo "${BLUE}🗺️ Opening density heat map...${NC}"
	open http://localhost:8080/density-map
	@echo "${GREEN}▶️  Watch regions update every minute${NC}"

## Demo Scenario 3: REST vs Stream Comparison
demo-comparison: ## Side-by-side REST vs Stream comparison
	@echo "${BLUE}⚖️ Starting REST vs Stream Comparison${NC}"
	@echo "${YELLOW}Setting up traditional batch approach...${NC}"
	./scripts/setup-batch-job.sh
	@echo "${YELLOW}Setting up streaming approach...${NC}"
	./gradlew :flink-jobs:runComparison &
	@echo "${GREEN}▶️  Available comparisons:${NC}"
	@echo "   make demo-batch-query     # Run traditional batch query (slow)"
	@echo "   make demo-stream-query    # Query streaming results (fast)"
	@echo "   make demo-freshness-test  # Show data freshness difference"

demo-batch-query: ## Simulate traditional batch processing (slow)
	@echo "${RED}🐌 Running traditional batch query...${NC}"
	@echo "${YELLOW}⏳ Simulating 2-second database query...${NC}"
	sleep 2
	@echo "${RED}📊 Results from last hour's batch job:${NC}"
	./scripts/query-batch-results.sh

demo-stream-query: ## Query real-time streaming results (fast)
	@echo "${GREEN}⚡ Querying real-time streaming results...${NC}"
	curl -s http://localhost:8080/api/density | jq '.[0:5]'
	@echo "${GREEN}📊 Results are live and current!${NC}"

demo-freshness-test: ## Demonstrate data freshness difference
	@echo "${BLUE}🕐 Data Freshness Comparison:${NC}"
	@echo "${RED}Batch approach: $(./scripts/get-batch-timestamp.sh)${NC}"
	@echo "${GREEN}Stream approach: $(./scripts/get-stream-timestamp.sh)${NC}"
	@echo "${YELLOW}Freshness difference: $(./scripts/calculate-freshness-diff.sh) seconds${NC}"

## Demo Scenario 4: Complete End-to-End Flow
demo-complete: ## Full end-to-end demo with all components
	@echo "${BLUE}🚀 Starting Complete End-to-End Demo${NC}"
	@echo "${YELLOW}Starting all components...${NC}"
	./gradlew :simulator:run &
	sleep 2
	./gradlew :flink-jobs:runAll &
	sleep 3
	./gradlew :ktor-service:run &
	sleep 2
	@echo "${GREEN}✅ All systems running!${NC}"
	@echo "${BLUE}▶️  Demonstrating complete flow:${NC}"
	make demo-show-flow

demo-show-flow: ## Show complete data flow in action
	@echo "${BLUE}📡 1. Generating flight event...${NC}"
	./scripts/generate-single-event.sh
	@echo "${BLUE}⚙️ 2. Flink processing...${NC}"
	sleep 1
	@echo "${BLUE}💾 3. PostgreSQL update...${NC}"
	sleep 1
	@echo "${BLUE}🌐 4. REST API response...${NC}"
	curl -s http://localhost:8080/api/latest | jq '.'
	@echo "${BLUE}📱 5. WebSocket push...${NC}"
	./scripts/show-websocket-update.sh
	@echo "${GREEN}✅ Complete flow: Event → Flink → DB → REST → WebSocket (< 2 seconds)${NC}"

## Monitoring Commands
monitor: ## Open monitoring dashboards
	@echo "${BLUE}📈 Opening monitoring dashboards...${NC}"
	open http://localhost:8081     # Flink Web UI
	open http://localhost:8080     # Ktor Service
	open http://localhost:5432     # PostgreSQL (pgAdmin if available)

logs: ## Show logs from all services
	@echo "${BLUE}📋 Showing service logs...${NC}"
	docker-compose -f docker/docker-compose.yml logs -f

logs-flink: ## Show only Flink job logs
	@echo "${BLUE}📋 Flink job logs:${NC}"
	./gradlew :flink-jobs:logs

logs-ktor: ## Show only Ktor service logs  
	@echo "${BLUE}📋 Ktor service logs:${NC}"
	./gradlew :ktor-service:logs

## Utility Commands
reset-demo: ## Reset demo to initial state (keep services running)
	@echo "${YELLOW}🔄 Resetting demo state...${NC}"
	./scripts/clear-kafka-topics.sh
	./scripts/clear-database.sh
	pkill -f "FlightEventGenerator" || true
	pkill -f "FlightProcessingJob" || true
	@echo "${GREEN}✅ Demo reset complete${NC}"

status: ## Show status of all demo components
	@echo "${BLUE}📊 Demo Component Status:${NC}"
	@echo "${YELLOW}Docker Services:${NC}"
	docker-compose -f docker/docker-compose.yml ps
	@echo "${YELLOW}Gradle Processes:${NC}"
	pgrep -f gradle | wc -l | xargs echo "  Active Gradle processes:"
	@echo "${YELLOW}Kafka Topics:${NC}"
	./scripts/list-kafka-topics.sh
	@echo "${YELLOW}Database Tables:${NC}"
	./scripts/list-database-tables.sh

## Demo Scripts for Specific Flink Use Cases
flink-delay-detection: ## Run only delay detection Flink job
	@echo "${BLUE}🔍 Starting Delay Detection Flink Job...${NC}"
	./gradlew :flink-jobs:runDelayDetection
	
flink-density-aggregation: ## Run only density aggregation Flink job  
	@echo "${BLUE}📊 Starting Density Aggregation Flink Job...${NC}"
	./gradlew :flink-jobs:runDensityAggregation

flink-complete-pipeline: ## Run complete Flink processing pipeline
	@echo "${BLUE}⚙️ Starting Complete Flink Pipeline...${NC}"
	./gradlew :flink-jobs:runComplete

## Presentation Helper Commands
demo-reset-for-talk: ## Reset everything for clean presentation start
	@echo "${RED}🎭 Preparing for presentation...${NC}"
	make clean
	make setup
	@echo "${GREEN}✅ Ready for talk! Start with 'make demo-interactive'${NC}"

demo-panic-recovery: ## Emergency recovery if demo breaks
	@echo "${RED}🚨 Emergency demo recovery...${NC}"
	pkill -f java || true
	docker-compose -f docker/docker-compose.yml restart
	sleep 10
	make demo-complete
	@echo "${GREEN}✅ Demo recovered!${NC}"
```

### Supporting Scripts Structure
```
scripts/
├── setup/
│   ├── wait-for-services.sh      # Wait for Docker services
│   ├── create-topics.sh          # Create Kafka topics  
│   └── init-database.sh          # Initialize PostgreSQL
├── demo/
│   ├── show-kafka-events.sh      # Display live Kafka events
│   ├── show-flink-output.sh      # Show Flink job output
│   ├── show-db-updates.sh        # Monitor DB changes
│   ├── show-flink-windows.sh     # Visualize windowing
│   └── generate-single-event.sh  # Create test event
├── monitoring/
│   ├── get-batch-timestamp.sh    # Get batch data timestamp
│   ├── get-stream-timestamp.sh   # Get stream data timestamp
│   └── calculate-freshness-diff.sh # Calculate freshness gap
└── utils/
    ├── clear-kafka-topics.sh     # Clean Kafka state
    ├── clear-database.sh         # Clean DB state
    └── list-kafka-topics.sh      # List topics
```

---

## Implementation Timeline (Simplified)

### Week 1: Basic Infrastructure
- [ ] Simple Kafka + PostgreSQL setup
- [ ] Basic flight event generator
- [ ] Simple JSON schema definition

### Week 2: Flink Processing  
- [ ] Basic delay detection job
- [ ] Simple density aggregation
- [ ] PostgreSQL sink configuration

### Week 3: Ktor Service
- [ ] REST endpoints for current state
- [ ] WebSocket for live updates
- [ ] Simple frontend dashboard

### Week 4: Demo Polish
- [ ] Demo scenarios and scripts
- [ ] Error handling and monitoring
- [ ] Documentation and setup guides

---

## Simplified Success Metrics

### Technical Performance
- **Event Processing**: Handle 100+ events/second
- **API Response Time**: <500ms for all endpoints
- **Update Frequency**: Database updates every 10 seconds
- **Demo Reliability**: Zero-config startup with docker-compose

### Demo Effectiveness  
- **Simplicity**: Anyone can run `make setup && make demo`
- **Clarity**: Clear difference between batch and stream processing
- **Replicability**: Complete code available on GitHub
- **Understanding**: Focus on concepts, not configuration

---

## What We Removed (And Why)

### Schema Registry & Avro
- **Why Removed**: Adds complexity without demonstrating core concepts
- **Benefit**: Focus on data flow rather than serialization details
- **Demo Impact**: Cleaner, more understandable message flow

### Complex Windowing
- **Why Removed**: Simple tumbling windows are sufficient for demonstration
- **Benefit**: Easier to explain and visualize
- **Demo Impact**: Clear understanding of time-based aggregation

### Advanced State Management
- **Why Removed**: PostgreSQL materialization is more familiar to developers
- **Benefit**: Leverages existing database knowledge
- **Demo Impact**: Shows integration with existing infrastructure

### Operational Complexity
- **Why Removed**: Monitoring, security, scaling are deployment concerns
- **Benefit**: Focus on development experience
- **Demo Impact**: Developers can focus on building, not operating

---

**Document Status**: Simplified v2.0  
**Next Review**: Implementation kickoff  
**Target Audience**: Kotlin developers new to streaming concepts

---

## Target Audience

### Primary: Kotlin/Ktor Developers
- **Experience Level**: Intermediate to Senior
- **Background**: Familiar with REST APIs, microservices, basic Kotlin
- **Knowledge Gaps**: Stream processing concepts, real-time architectures
- **Goals**: Learn practical streaming techniques they can apply immediately

### Secondary: General Backend Developers
- **Interests**: Performance, scalability, modern architectures
- **Concerns**: Complexity, operational overhead, learning curve

---

## Core Features

### 1. Real-Time Flight Event Processing
**User Story**: *As an air traffic controller, I want to see flight movements and status changes in real-time so I can make informed decisions quickly.*

#### Data Sources
- **Flight Position Updates**: GPS coordinates, altitude, speed
- **Status Changes**: Takeoff, landing, delay, cancellation events
- **Weather Events**: Conditions affecting flight operations
- **Airport Events**: Gate changes, boarding status

#### Event Schema
```json
{
  "flight_id": "LH441",
  "airline": "Lufthansa", 
  "event_type": "POSITION_UPDATE|DELAY|TAKEOFF|LANDING",
  "timestamp": "2025-05-23T14:30:00Z",
  "latitude": 50.0379,
  "longitude": 8.5622,
  "altitude": 35000,
  "speed": 850,
  "delay_minutes": 0,
  "origin": "FRA",
  "destination": "JFK",
  "aircraft_type": "A380"
}
```

### 2. Geographic Flight Density Monitoring
**User Story**: *As an operations manager, I want to see flight density by geographic region updated every minute so I can identify congestion patterns.*

#### Requirements
- **Grid System**: 5°×5° geographic regions
- **Update Frequency**: Every 1 minute tumbling windows
- **Metrics Tracked**: 
  - Active flights per region
  - Average altitude per region
  - Traffic trends over time
- **Visualization**: Heat map overlay on world map

#### Flink Implementation
```sql
CREATE VIEW flight_density AS
SELECT 
    FLOOR(latitude/5)*5 as grid_lat,
    FLOOR(longitude/5)*5 as grid_lon,
    COUNT(*) as flight_count,
    AVG(altitude) as avg_altitude,
    TUMBLE_START(event_time, INTERVAL '1' MINUTE) as window_start,
    TUMBLE_END(event_time, INTERVAL '1' MINUTE) as window_end
FROM flights_topic
WHERE event_type = 'POSITION_UPDATE'
GROUP BY 
    FLOOR(latitude/5)*5, 
    FLOOR(longitude/5)*5,
    TUMBLE(event_time, INTERVAL '1' MINUTE);
```

### 3. Delayed Flight Alert System
**User Story**: *As a passenger services coordinator, I want to be notified immediately when flights are delayed so I can proactively communicate with affected passengers.*

#### Requirements
- **Real-time Detection**: Alert within 5 seconds of delay event
- **Threshold Configuration**: Configurable delay thresholds (default: >15 minutes)
- **Alert Enrichment**: Include passenger count, gate info, rebooking options
- **Escalation Rules**: Different alerts for 30min, 1hr, 2hr+ delays

#### Flink Implementation
```sql
CREATE VIEW delayed_flights AS
SELECT 
    flight_id,
    airline,
    delay_minutes,
    origin,
    destination,
    estimated_passengers,
    delay_category,
    event_time
FROM flights_topic
WHERE delay_minutes > 15
```

### 4. Interactive Dashboard (Eye Candy & Visual Appeal)
**User Story**: *As a demo audience member, I want to see stunning visualizations that make real-time data feel magical and compelling.*

#### Design Philosophy: Premium iOS/macOS Aesthetic
- **Visual Hierarchy**: Ultra-clean design with generous white space and purposeful layouts
- **Typography**: SF Pro Display font family with carefully crafted text scales
- **Color Palette**: 
  - Primary: #007AFF (iOS System Blue) with subtle gradients
  - Success: #34C759 (iOS Green) for positive metrics
  - Warning: #FF9500 (iOS Orange) for delays
  - Critical: #FF3B30 (iOS Red) for alerts
  - Background: Dynamic light/dark mode support
  - Glass Effects: Translucent panels with backdrop blur
- **Micro-Animations**: Smooth 60fps transitions with spring physics
- **Shadows & Depth**: Subtle layering with realistic drop shadows

#### Premium Dashboard Components

##### 1. **Hero Map Visualization** 
```css
/* Stunning world map with real-time flight tracking */
.flight-map {
    background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
    border-radius: 20px;
    box-shadow: 0 20px 40px rgba(0,0,0,0.1);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(255,255,255,0.1);
}

.flight-icon {
    filter: drop-shadow(0 2px 8px rgba(0,122,255,0.3));
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.flight-icon:hover {
    transform: scale(1.2);
    filter: drop-shadow(0 4px 16px rgba(0,122,255,0.5));
}

.density-heatmap {
    background: radial-gradient(circle, rgba(0,122,255,0.8) 0%, rgba(0,122,255,0.2) 70%);
    animation: pulse 2s ease-in-out infinite;
}
```

##### 2. **Animated Metrics Cards**
```css
.metric-card {
    background: rgba(255,255,255,0.9);
    backdrop-filter: blur(20px);
    border-radius: 16px;
    padding: 24px;
    box-shadow: 
        0 8px 32px rgba(0,0,0,0.1),
        inset 0 1px 0 rgba(255,255,255,0.5);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.metric-card:hover {
    transform: translateY(-4px);
    box-shadow: 
        0 20px 40px rgba(0,0,0,0.15),
        inset 0 1px 0 rgba(255,255,255,0.5);
}

.metric-number {
    font-size: 3rem;
    font-weight: 700;
    background: linear-gradient(135deg, #007AFF, #5856D6);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    animation: countUp 1s ease-out;
}

@keyframes countUp {
    from { transform: scale(0.5); opacity: 0; }
    to { transform: scale(1); opacity: 1; }
}
```

##### 3. **Live Alert Feed with iOS-style Notifications**
```css
.alert-notification {
    background: rgba(255,59,48,0.95);
    backdrop-filter: blur(20px);
    border-radius: 12px;
    padding: 16px 20px;
    margin: 8px 0;
    box-shadow: 0 4px 20px rgba(255,59,48,0.3);
    animation: slideInRight 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    border-left: 4px solid #FF3B30;
}

@keyframes slideInRight {
    from { 
        transform: translateX(100%); 
        opacity: 0; 
    }
    to { 
        transform: translateX(0); 
        opacity: 1; 
    }
}

.alert-icon {
    width: 24px;
    height: 24px;
    background: linear-gradient(135deg, #FF6B6B, #FF3B30);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    animation: bounce 0.6s ease-in-out;
}

@keyframes bounce {
    0%, 20%, 53%, 80%, 100% { transform: translate3d(0,0,0); }
    40%, 43% { transform: translate3d(0,-8px,0); }
    70% { transform: translate3d(0,-4px,0); }
    90% { transform: translate3d(0,-2px,0); }
}
```

##### 4. **Real-time Data Flow Visualization**
```css
.data-flow-container {
    position: relative;
    height: 120px;
    background: linear-gradient(90deg, 
        rgba(0,122,255,0.1) 0%, 
        rgba(88,86,214,0.1) 50%, 
        rgba(52,199,89,0.1) 100%);
    border-radius: 16px;
    overflow: hidden;
}

.data-particle {
    position: absolute;
    width: 8px;
    height: 8px;
    background: radial-gradient(circle, #007AFF 0%, #5856D6 100%);
    border-radius: 50%;
    box-shadow: 0 0 16px rgba(0,122,255,0.6);
    animation: flowRight 3s linear infinite;
}

@keyframes flowRight {
    0% { 
        transform: translateX(-20px) scale(0); 
        opacity: 0; 
    }
    10% { 
        transform: translateX(0) scale(1); 
        opacity: 1; 
    }
    90% { 
        transform: translateX(calc(100vw - 100px)) scale(1); 
        opacity: 1; 
    }
    100% { 
        transform: translateX(calc(100vw - 80px)) scale(0); 
        opacity: 0; 
    }
}
```

##### 5. **Beautiful Chart Visualizations**
```css
.chart-container {
    background: rgba(255,255,255,0.95);
    backdrop-filter: blur(20px);
    border-radius: 20px;
    padding: 32px;
    box-shadow: 
        0 16px 40px rgba(0,0,0,0.1),
        inset 0 1px 0 rgba(255,255,255,0.8);
}

.chart-line {
    stroke: url(#gradient);
    stroke-width: 3;
    fill: none;
    filter: drop-shadow(0 2px 8px rgba(0,122,255,0.3));
    animation: drawLine 2s ease-out;
}

@keyframes drawLine {
    from { stroke-dasharray: 1000; stroke-dashoffset: 1000; }
    to { stroke-dasharray: 1000; stroke-dashoffset: 0; }
}

/* SVG gradient definition */
<defs>
    <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%" style="stop-color:#007AFF;stop-opacity:1" />
        <stop offset="100%" style="stop-color:#5856D6;stop-opacity:1" />
    </linearGradient>
</defs>
```

#### Interactive UI Elements

##### **Elegant Control Panel**
```css
.control-panel {
    background: rgba(28,28,30,0.95);
    backdrop-filter: blur(20px);
    border-radius: 20px;
    padding: 24px;
    border: 1px solid rgba(255,255,255,0.1);
}

.demo-button {
    background: linear-gradient(135deg, #007AFF 0%, #5856D6 100%);
    border: none;
    border-radius: 12px;
    padding: 12px 24px;
    color: white;
    font-weight: 600;
    box-shadow: 0 4px 16px rgba(0,122,255,0.3);
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    overflow: hidden;
}

.demo-button:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 25px rgba(0,122,255,0.4);
}

.demo-button:active {
    transform: translateY(0);
    box-shadow: 0 2px 8px rgba(0,122,255,0.3);
}

/* Ripple effect on click */
.demo-button::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    border-radius: 50%;
    background: rgba(255,255,255,0.3);
    transform: translate(-50%, -50%);
    transition: width 0.6s, height 0.6s;
}

.demo-button:active::after {
    width: 300px;
    height: 300px;
}
```

##### **Smooth Toggle Switches**
```css
.toggle-switch {
    position: relative;
    width: 60px;
    height: 34px;
    background: rgba(120,120,128,0.16);
    border-radius: 17px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    cursor: pointer;
}

.toggle-switch.active {
    background: #34C759;
    box-shadow: 0 0 20px rgba(52,199,89,0.4);
}

.toggle-knob {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 30px;
    height: 30px;
    background: white;
    border-radius: 50%;
    box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toggle-switch.active .toggle-knob {
    transform: translateX(26px);
    box-shadow: 0 2px 12px rgba(0,0,0,0.2);
}
```

#### Responsive Design with Visual Polish

##### **Mobile-First Responsive Grid**
```css
.dashboard-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 24px;
    padding: 24px;
    max-width: 1400px;
    margin: 0 auto;
}

@media (max-width: 768px) {
    .dashboard-grid {
        grid-template-columns: 1fr;
        gap: 16px;
        padding: 16px;
    }
    
    .metric-card {
        padding: 20px;
    }
    
    .metric-number {
        font-size: 2.5rem;
    }
}

/* Touch-friendly interactions */
@media (hover: none) {
    .metric-card:hover {
        transform: none;
    }
    
    .demo-button:hover {
        transform: none;
    }
}
```

##### **Dark Mode Support**
```css
@media (prefers-color-scheme: dark) {
    .metric-card {
        background: rgba(28,28,30,0.9);
        color: white;
        box-shadow: 
            0 8px 32px rgba(0,0,0,0.3),
            inset 0 1px 0 rgba(255,255,255,0.1);
    }
    
    .flight-map {
        background: linear-gradient(135deg, #0a0a0a 0%, #1a1a1a 100%);
        border: 1px solid rgba(255,255,255,0.05);
    }
    
    .control-panel {
        background: rgba(44,44,46,0.95);
        border: 1px solid rgba(255,255,255,0.05);
    }
}
```

#### Performance Optimizations for Smooth Animations

```css
/* Hardware acceleration for smooth animations */
.metric-card,
.flight-icon,
.data-particle,
.demo-button {
    will-change: transform;
    transform: translateZ(0);
}

/* Reduce motion for accessibility */
@media (prefers-reduced-motion: reduce) {
    * {
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
    }
}

/* High refresh rate support */
@media (min-resolution: 120dpi) {
    .metric-number {
        text-rendering: optimizeLegibility;
        -webkit-font-smoothing: antialiased;
    }
}
```

#### Interactive Demo Features

##### **Audience Engagement Elements**
- **Live Audience Counter**: Show number of connected viewers
- **Real-time Poll Integration**: "Which approach do you prefer?" voting
- **Demo Progress Bar**: Visual indicator of presentation progress
- **QR Code**: For audience to join live dashboard on their phones
- **Sound Effects**: Subtle audio feedback for major events (optional)

##### **Presenter Tools**
- **Demo Scenario Selector**: Quick buttons to jump between scenarios
- **Speed Controls**: Slow down or speed up event generation
- **Highlight Mode**: Emphasize specific UI elements during explanation
- **Presentation Timer**: Keep track of demo timing
- **Emergency Reset**: One-click recovery from demo failures

### 5. REST API Layer
**User Story**: *As a mobile app developer, I want REST endpoints that return current flight information so I can build responsive applications.*

#### Core Endpoints

##### Current State Queries
```kotlin
GET /api/flights/current
// Returns snapshot of all active flights

GET /api/flights/region/{lat}/{lon}
// Returns current flights in specific 5x5 degree region

GET /api/flights/delayed
// Returns currently delayed flights

GET /api/flights/density/current
// Returns current flight density grid
```

##### Historical Queries
```kotlin
GET /api/flights/density/history?hours=24
// Returns density data for last N hours

GET /api/flights/{flightId}/history
// Returns position history for specific flight
```

##### System Status
```kotlin
GET /api/system/health
// Returns processing lag, throughput, error rates

GET /api/system/metrics
// Returns Flink job statistics
```

---

## Technical Architecture

### System Overview
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Data Source   │───▶│   Apache Kafka   │───▶│  Apache Flink   │
│   (Simulator)   │    │    (Events)      │    │  (Processing)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                                                         ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Web Dashboard  │◀───│   Ktor Service   │◀───│  Processed Data │
│   (Frontend)    │    │  (REST/WebSocket)│    │   (Kafka/State) │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

### Technology Stack

#### Stream Processing Layer
- **Apache Flink 2.0**: Core stream processing engine with enhanced Table API
- **Flink Table API**: SQL-based stream definitions (following [official docs](https://nightlies.apache.org/flink/flink-docs-release-2.0/docs/dev/table/overview/))
- **Confluent Platform 7.9.0**: Complete streaming platform
- **Confluent Schema Registry**: Avro schema management and evolution

#### Application Layer  
- **Ktor 2.3+**: Web framework and REST API
- **Kotlin 1.9+**: Primary programming language (following [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html))
- **Gradle with Kotlin DSL**: Build automation and dependency management
- **Kotlinx.serialization**: JSON handling
- **Exposed/R2DBC**: Database integration (for materialized views)

#### Frontend Layer
- **Modern UI Framework**: iOS/macOS inspired design system
- **TypeScript**: Type-safe client-side development
- **WebSocket API**: Real-time updates
- **Leaflet.js**: Interactive maps with custom styling
- **Chart.js/D3.js**: Beautiful metrics visualizations
- **Responsive Design**: Mobile-first, adaptive layouts
- **CSS Grid/Flexbox**: Modern layout techniques

#### Infrastructure & Automation
- **Docker Compose**: Local development environment
- **GitHub Actions**: CI/CD pipeline for builds and testing
- **Renovate**: Automated dependency updates
- **Testcontainers**: Integration testing
- **Micrometer**: Metrics collection
- **Logback**: Logging
- **Makefile**: Demo scenario orchestration

### Data Flow Architecture

#### 1. Event Ingestion
```kotlin
// Flight event simulator
class FlightEventSimulator {
    fun generateEvents(): Flow<FlightEvent> = flow {
        while (true) {
            emit(generateRealisticFlightEvent())
            delay(Random.nextLong(100, 1000)) // Variable timing
        }
    }
}

// Kafka producer
class FlightEventProducer(private val producer: KafkaProducer<String, FlightEvent>) {
    suspend fun publish(event: FlightEvent) {
        producer.send(ProducerRecord("flights_topic", event.flightId, event))
    }
}
```

#### 2. Stream Processing Jobs
```kotlin
// Flink Table API job definition (following Flink 2.0 specifications)
object FlightProcessingJobs {
    fun createDensityJob(): StreamExecutionEnvironment {
        val env = StreamExecutionEnvironment.getExecutionEnvironment()
        val tableEnv = StreamTableEnvironment.create(env)
        
        // Configure Flink 2.0 optimizations
        tableEnv.config.set("table.exec.source.idle-timeout", "30s")
        tableEnv.config.set("table.optimizer.join-reorder-enabled", "true")
        
        // Register Confluent Kafka source with Avro
        tableEnv.executeSql("""
            CREATE TABLE flights_source (
                flight_id STRING,
                airline STRING,
                latitude DOUBLE,
                longitude DOUBLE, 
                altitude INT,
                delay_minutes INT,
                event_type STRING,
                event_time TIMESTAMP_LTZ(3),
                WATERMARK FOR event_time AS event_time - INTERVAL '5' SECOND
            ) WITH (
                'connector' = 'kafka',
                'topic' = 'flights_topic',
                'properties.bootstrap.servers' = 'localhost:9092',
                'properties.group.id' = 'flight-processor',
                'format' = 'avro-confluent',
                'avro-confluent.schema-registry.url' = 'http://localhost:8081'
            )
        """)
        
        // Register processed data sink
        tableEnv.executeSql("""
            CREATE TABLE processed_flights_sink (
                grid_lat INT,
                grid_lon INT,
                flight_count BIGINT,
                avg_altitude DOUBLE,
                window_start TIMESTAMP_LTZ(3),
                window_end TIMESTAMP_LTZ(3)
            ) WITH (
                'connector' = 'kafka',
                'topic' = 'processed_flights',
                'format' = 'avro-confluent',
                'avro-confluent.schema-registry.url' = 'http://localhost:8081'
            )
        """)
        
        // Create and execute processing view using Flink 2.0 Table API
        tableEnv.executeSql("""
            INSERT INTO processed_flights_sink
            SELECT 
                CAST(FLOOR(latitude/5)*5 AS INT) as grid_lat,
                CAST(FLOOR(longitude/5)*5 AS INT) as grid_lon,
                COUNT(*) as flight_count,
                AVG(CAST(altitude AS DOUBLE)) as avg_altitude,
                TUMBLE_START(event_time, INTERVAL '1' MINUTE) as window_start,
                TUMBLE_END(event_time, INTERVAL '1' MINUTE) as window_end
            FROM flights_source
            WHERE event_type = 'POSITION_UPDATE'
            GROUP BY 
                CAST(FLOOR(latitude/5)*5 AS INT), 
                CAST(FLOOR(longitude/5)*5 AS INT),
                TUMBLE(event_time, INTERVAL '1' MINUTE)
        """)
        
        return env
    }
}
```

#### 3. State Materialization Strategies

##### Strategy A: Queryable State
```kotlin
// Direct Flink state queries
@Service
class FlinkStateService {
    private val queryClient = QueryableStateClient(flinkJobManager, 9069)
    
    suspend fun getCurrentDensity(gridLat: Int, gridLon: Int): FlightDensity? {
        return queryClient.getKvState(
            jobId = flinkJobId,
            queryableStateName = "flight-density-state",
            key = GridKey(gridLat, gridLon),
            keyTypeInfo = TypeInformation.of(GridKey::class.java),
            stateTypeInfo = TypeInformation.of(FlightDensity::class.java)
        )
    }
}
```

##### Strategy B: Materialized Tables
```kotlin
// Flink maintains current state table
class MaterializedStateService {
    // Flink job continuously updates this table
    fun setupMaterializedView() {
        tableEnv.executeSql("""
            CREATE TABLE current_flight_density (
                grid_lat INT,
                grid_lon INT,
                flight_count BIGINT,
                last_updated TIMESTAMP(3),
                PRIMARY KEY (grid_lat, grid_lon) NOT ENFORCED
            ) WITH (
                'connector' = 'jdbc',
                'url' = 'jdbc:postgresql://localhost:5432/demo',
                'table-name' = 'current_flight_density'
            )
        """)
        
        // Insert/Update from stream
        tableEnv.executeSql("""
            INSERT INTO current_flight_density
            SELECT grid_lat, grid_lon, flight_count, CURRENT_TIMESTAMP
            FROM flight_density
        """)
    }
}
```

#### 4. REST API Implementation
```kotlin
// Ktor routing with materialized state
fun Application.configureRouting() {
    routing {
        route("/api") {
            // Current state endpoints
            get("/flights/density/current") {
                val density = materializedStateService.getCurrentDensity()
                call.respond(density)
            }
            
            get("/flights/delayed") {
                val delayed = materializedStateService.getDelayedFlights()
                call.respond(delayed)
            }
            
            // Real-time WebSocket
            webSocket("/flights/live") {
                val consumer = kafkaConsumerFactory.createConsumer<FlightUpdate>()
                consumer.subscribe(listOf("processed_flights"))
                
                try {
                    while (true) {
                        val records = consumer.poll(Duration.ofMillis(100))
                        records.forEach { record ->
                            send(Frame.Text(Json.encodeToString(record.value())))
                        }
                    }
                } finally {
                    consumer.close()
                }
            }
        }
    }
}
```

---

## Demo Scenarios

### Scenario 1: Normal Operations Baseline
**Duration**: 2 minutes  
**Purpose**: Establish normal flight patterns

**Script**:
1. Show steady flight traffic across major routes
2. Highlight consistent geographic density patterns
3. Demonstrate responsive dashboard updates
4. Query REST endpoints to show current state

**Expected Behavior**:
- 50-100 flights visible on map
- Density concentrates around major airports
- Smooth real-time updates every few seconds
- REST responses under 100ms

### Scenario 2: Weather Disruption Event  
**Duration**: 3 minutes  
**Purpose**: Demonstrate real-time alerting and pattern changes

**Script**:
1. Trigger simulated weather event over Europe
2. Watch delayed flight alerts cascade
3. Show geographic density shifting as flights reroute
4. Demonstrate alert escalation (15min → 30min → 1hr delays)

**Expected Behavior**:
- Immediate delay alerts (within 5 seconds)
- Geographic heat map shows traffic redistribution
- Alert feed shows escalating delay notifications
- Trend charts show impact over time

### Scenario 3: High Traffic Stress Test
**Duration**: 2 minutes  
**Purpose**: Show system performance under load

**Script**:
1. Ramp up event generation to 1000+ events/second
2. Monitor processing lag and throughput metrics
3. Verify dashboard remains responsive
4. Show REST endpoints maintain low latency

**Expected Behavior**:
- Processing lag stays under 1 second
- Dashboard updates remain smooth
- System metrics show healthy throughput
- No visible degradation to audience

### Scenario 4: REST vs Stream Comparison
**Duration**: 3 minutes  
**Purpose**: Drive home the core message

**Script**:
1. Show traditional "refresh button" approach simulation
2. Compare with live streaming updates
3. Demonstrate data freshness difference
4. Query both approaches side-by-side

**Expected Behavior**:
- Clear visual difference between batch vs. real-time
- Audience sees immediate value proposition
- Data freshness gap is obvious
- Performance difference is dramatic

---

## Implementation Plan

### Phase 1: Core Infrastructure (Week 1)
- [ ] Set up Kafka cluster with docker-compose
- [ ] Implement flight event simulator with realistic data
- [ ] Create basic Flink job for event processing
- [ ] Set up Ktor application skeleton

### Phase 2: Stream Processing (Week 2)  
- [ ] Implement geographic density windowing job
- [ ] Create delayed flight filtering job
- [ ] Set up Avro schemas and serialization
- [ ] Add Kafka sinks for processed data

### Phase 3: REST API Layer (Week 3)
- [ ] Implement materialized state storage
- [ ] Create REST endpoints for current state
- [ ] Add WebSocket endpoints for live updates
- [ ] Implement error handling and monitoring

### Phase 4: Dashboard Frontend (Week 4)
- [ ] Create interactive world map with Leaflet
- [ ] Implement real-time metrics panels
- [ ] Add alert feed and notification system
- [ ] Build demo control panel

### Phase 5: Demo Scenarios (Week 5)
- [ ] Create scripted demo scenarios
- [ ] Add scenario trigger mechanisms
- [ ] Implement demo monitoring and fallbacks
- [ ] Performance tune for presentation environment

### Phase 6: Testing & Polish (Week 6)
- [ ] Load testing with realistic traffic volumes
- [ ] End-to-end integration testing
- [ ] Demo rehearsals and script refinement
- [ ] Documentation and setup guides

## Non-Functional Requirements

### Code Quality & Standards
- **Kotlin Coding Standards**: Strict adherence to [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
  - 4-space indentation
  - Maximum line length: 120 characters
  - Naming conventions: camelCase for functions/variables, PascalCase for classes
  - No wildcard imports
  - Explicit type declarations for public APIs
- **Code Coverage**: Minimum 80% test coverage for critical components
- **Static Analysis**: ktlint and detekt integration with zero violations policy
- **Documentation**: KDoc for all public APIs and complex business logic

### Performance Requirements
- **Event Processing Latency**: <200ms end-to-end (P95)
- **REST API Response Time**: <100ms for all endpoints (P99)
- **WebSocket Connection Stability**: >99.9% uptime during demo
- **Throughput**: Handle 1000+ events/second sustained load
- **Memory Usage**: <4GB heap for Flink jobs under normal load
- **Frontend Load Time**: <3 seconds initial page load
- **UI Responsiveness**: 60fps animations, <16ms frame time

### Scalability Requirements
- **Horizontal Scaling**: Flink jobs support parallelism up to 8 task slots
- **Data Retention**: Process 24 hours of historical data without degradation
- **Concurrent Users**: Support 50+ simultaneous dashboard connections
- **Event Backlog**: Handle up to 1 million events in Kafka topics

### Reliability Requirements
- **Fault Tolerance**: Automatic recovery from single component failures
- **Data Consistency**: Exactly-once processing guarantees
- **Demo Resilience**: <5 second recovery from any single point of failure
- **Graceful Degradation**: System remains functional with reduced features during partial outages

### Security Requirements
- **API Security**: Basic authentication for admin endpoints
- **Data Privacy**: No real personal information in demo data
- **CORS Configuration**: Proper cross-origin resource sharing setup
- **Input Validation**: Sanitize all user inputs and API parameters

### DevOps & CI/CD Requirements

#### GitHub Actions Workflow
```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Setup Gradle
      uses: gradle/gradle-build-action@v2
    - name: Run tests
      run: ./gradlew test
    - name: Code coverage
      run: ./gradlew jacocoTestReport
    - name: Static analysis
      run: ./gradlew ktlintCheck detekt
    - name: Build Docker images
      run: make build-images
    - name: Integration tests
      run: make test-integration
```

#### Renovate Configuration
```json
{
  "extends": ["config:base"],
  "packageRules": [
    {
      "matchPackagePatterns": ["^org.apache.flink"],
      "groupName": "Flink dependencies"
    },
    {
      "matchPackagePatterns": ["^io.confluent"],
      "groupName": "Confluent dependencies"
    },
    {
      "matchPackagePatterns": ["^io.ktor"],
      "groupName": "Ktor dependencies"
    }
  ],
  "schedule": ["before 6am on Monday"],
  "timezone": "UTC",
  "labels": ["dependencies"]
}
```

#### Build System Configuration
```kotlin
// build.gradle.kts (root project)
plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("org.gradle.test-retry") version "1.5.8"
    id("jacoco")
}

allprojects {
    group = "com.flightdemo"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jlleitschuh.gradle.ktlint")
        plugin("io.gitlab.arturbosch.detekt")
        plugin("jacoco")
    }
    
    dependencies {
        implementation(platform("org.apache.flink:flink-bom:2.0.0"))
        implementation(platform("io.confluent:confluent-bom:7.9.0"))
        implementation(platform("io.ktor:ktor-bom:2.3.8"))
        
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
        testImplementation("org.testcontainers:junit-jupiter:1.19.3")
        testImplementation("org.testcontainers:kafka:1.19.3")
    }
    
    kotlin {
        jvmToolchain(17)
    }
    
    tasks.test {
        useJUnitPlatform()
        retry {
            maxRetries.set(2)
            maxFailures.set(3)
        }
    }
}
```

#### Demo Orchestration Makefile
```makefile
# Makefile for demo scenarios
.PHONY: help setup demo-* clean

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-20s %s\n", $1, $2}' $(MAKEFILE_LIST)

setup: ## Setup development environment
	@echo "🚀 Setting up flight demo environment..."
	docker-compose -f docker/docker-compose.yml up -d
	@echo "⏳ Waiting for services to be ready..."
	./scripts/wait-for-services.sh
	@echo "📊 Creating Kafka topics..."
	./scripts/create-topics.sh
	@echo "✅ Environment ready!"

demo-baseline: ## Run baseline demo scenario
	@echo "📊 Starting baseline scenario..."
	./scripts/scenarios/baseline.sh

demo-weather-disruption: ## Run weather disruption scenario  
	@echo "🌩️ Starting weather disruption scenario..."
	./scripts/scenarios/weather-disruption.sh

demo-high-traffic: ## Run high traffic stress test
	@echo "🚦 Starting high traffic scenario..."
	./scripts/scenarios/high-traffic.sh

demo-comparison: ## Run REST vs Stream comparison
	@echo "⚖️ Starting comparison scenario..."
	./scripts/scenarios/rest-vs-stream.sh

build-images: ## Build all Docker images
	@echo "🏗️ Building Docker images..."
	docker build -t flight-demo/simulator ./simulator
	docker build -t flight-demo/flink-jobs ./flink-jobs  
	docker build -t flight-demo/ktor-service ./ktor-service

test-integration: ## Run integration tests
	@echo "🧪 Running integration tests..."
	./gradlew integrationTest

clean: ## Clean up environment
	@echo "🧹 Cleaning up..."
	docker-compose -f docker/docker-compose.yml down -v
	docker system prune -f

monitor: ## Open monitoring dashboard
	@echo "📈 Opening monitoring dashboard..."
	open http://localhost:3000 # Grafana
	open http://localhost:8080 # Flink UI
	open http://localhost:9021 # Confluent Control Center
```

### Technical Risks

#### Demo Environment Failures
**Risk**: Network issues, container crashes during live demo  
**Mitigation**: 
- Pre-recorded demo video as backup
- Local development environment with sample data
- Health check endpoints with automatic recovery
- Multiple demo scenarios prepared

#### Performance Issues
**Risk**: System can't handle demo load, visible lag  
**Mitigation**:
- Load testing with 10x expected demo traffic
- Performance monitoring with automatic alerts
- Fallback to smaller dataset if needed
- Pre-warmed system state

#### Data Quality Issues
**Risk**: Unrealistic flight patterns, obvious fake data  
**Mitigation**:
- Use real airport codes and flight routes
- Implement realistic flight timing and patterns
- Add natural variance and edge cases
- Test with aviation domain experts

### Presentation Risks

#### Audience Engagement
**Risk**: Complex technical concepts lose audience attention  
**Mitigation**:
- Start with relatable airport story
- Use progressive disclosure of technical details
- Include interactive polling and questions
- Visual demonstrations over code explanations

#### Time Management
**Risk**: Demo runs long, insufficient Q&A time  
**Mitigation**:
- Practice timed run-throughs
- Have shortened demo version ready
- Clear checkpoint timing throughout talk
- Backup slides for key concepts if demo fails

---

## Success Metrics & KPIs

### Technical Performance
- **Event Processing Latency**: <200ms end-to-end
- **REST Response Time**: <100ms for all endpoints
- **WebSocket Connection Stability**: >99% uptime during demo
- **Data Accuracy**: 100% event processing (no data loss)

### Demo Effectiveness
- **Audience Engagement**: Post-talk survey scores >4.5/5
- **Concept Understanding**: 80%+ of attendees can explain stream vs. REST difference
- **Follow-up Interest**: >50% of attendees visit demo GitHub repo
- **Technical Adoption**: 25%+ report planning to try similar approaches

### Speaker Experience
- **Demo Reliability**: <5 seconds recovery from any failure
- **Presentation Flow**: Smooth transitions between concepts and code
- **Time Management**: Complete demo within allocated 10-minute window
- **Backup Readiness**: All fallback scenarios tested and working

---

## Appendix

### A. Technical Specifications

#### Hardware Requirements
- **Development**: 16GB RAM, 8 CPU cores, 100GB disk
- **Demo Environment**: 32GB RAM, 16 CPU cores, 200GB SSD
- **Network**: Stable internet for Kafka/container downloads

#### Software Dependencies
```yaml
# docker-compose.yml excerpt for Confluent Platform 7.9.0
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.9.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.9.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: 'zookeeper:2181'
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1

  schema-registry:
    image: confluentinc/cp-schema-registry:7.9.0
    depends_on:
      - kafka
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: 'kafka:29092'
      SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081

  control-center:
    image: confluentinc/cp-enterprise-control-center:7.9.0
    depends_on:
      - kafka
      - schema-registry
    environment:
      CONTROL_CENTER_BOOTSTRAP_SERVERS: 'kafka:29092'
      CONTROL_CENTER_SCHEMA_REGISTRY_URL: "http://schema-registry:8081"
      CONTROL_CENTER_REPLICATION_FACTOR: 1
      CONTROL_CENTER_INTERNAL_TOPICS_PARTITIONS: 1
      CONTROL_CENTER_MONITORING_INTERCEPTOR_TOPIC_PARTITIONS: 1

  flink-jobmanager:
    image: flink:2.0-scala_2.12
    command: jobmanager
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 8
        parallelism.default: 4
        state.backend: hashmap
        state.checkpoints.dir: file:///tmp/flink-checkpoints
        execution.checkpointing.interval: 60000
        
  flink-taskmanager:
    image: flink:2.0-scala_2.12
    depends_on:
      - flink-jobmanager
    command: taskmanager
    scale: 2
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 8
        taskmanager.memory.process.size: 2048m

  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: flight_demo
      POSTGRES_USER: demo
      POSTGRES_PASSWORD: demo123
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### B. Demo Script Template

#### Opening (30 seconds)
> "Remember my Frankfurt airport story? Let me show you the dashboard I wish I had that day. We're going to build a real-time flight control center that updates faster than my frustrated F5 refreshing."

#### Setup (60 seconds)  
> "Here's our starting point - flights are generating events right now. Position updates, delays, weather changes. Just like a real airport, but compressed into demo time."

#### Stream Processing Demo (3 minutes)
> "Watch this Flink SQL - it's creating geographic regions and counting flights in real-time. Every minute, we get fresh density data. And here's the delayed flight filter - any delay over 15 minutes triggers an immediate alert."

#### REST Integration (2 minutes)
> "But here's the magic - we can still query current state instantly. No database queries, no stale cache. The stream keeps our REST API fresh automatically."

#### Scenario Execution (3 minutes)
> "Let me trigger a weather event over Europe... watch the delay alerts cascade... see how the geographic density shifts as flights reroute... this is real-time decision support."

#### Closing (90 seconds)
> "In 5 minutes, you've seen events flow from simulation through Flink processing to live dashboard updates. Sub-200ms latency, always fresh data, REST when you need it, streams when you want it. This is what that airport employee needed - not phone calls to humans, just truth, in real-time."

### C. Resource Links

#### Code Repository Structure
```
flight-demo/
├── .github/
│   ├── workflows/          # GitHub Actions CI/CD
│   └── renovate.json       # Dependency management config
├── docker/
│   ├── docker-compose.yml  # Confluent Platform 7.9.0 setup
│   └── configs/            # Service configurations
├── flink-jobs/
│   ├── build.gradle.kts    # Flink 2.0 dependencies
│   ├── src/main/kotlin/    # Stream processing jobs
│   └── src/main/resources/ # Avro schemas
├── ktor-service/
│   ├── build.gradle.kts    # Ktor and serialization deps
│   ├── src/main/kotlin/    # REST API and WebSocket server
│   └── src/main/resources/ # Configuration files
├── frontend/
│   ├── src/
│   │   ├── styles/         # iOS-inspired CSS/SCSS
│   │   ├── components/     # TypeScript UI components
│   │   └── utils/          # Client-side utilities
│   ├── package.json        # Node.js dependencies
│   └── tsconfig.json       # TypeScript configuration
├── simulator/
│   ├── build.gradle.kts    # Event generator dependencies
│   └── src/main/kotlin/    # Flight event simulation
├── scripts/
│   ├── scenarios/          # Demo scenario scripts
│   ├── setup/              # Environment setup scripts
│   └── monitoring/         # Health check utilities
├── docs/
│   ├── api/                # REST API documentation
│   ├── deployment/         # Setup and deployment guides
│   └── demo/               # Demo execution guides
├── Makefile                # Demo orchestration commands
├── build.gradle.kts        # Root project configuration
├── gradle.properties       # Gradle and Kotlin versions
├── settings.gradle.kts     # Multi-project setup
└── renovate.json           # Dependency update automation
```

#### External Dependencies
- [Apache Flink Documentation](https://flink.apache.org/docs/)
- [Ktor Framework Guide](https://ktor.io/docs/)
- [Kafka Quick Start](https://kafka.apache.org/quickstart)
- [Leaflet.js Mapping Library](https://leafletjs.com/)

---

**Document Status**: Draft v1.0  
**Next Review**: Implementation kickoff meeting  
**Approvers**: Technical Lead, Conference Program Committee