# Flight Control Center Demo Application Implementation Plan

## Overview

This document outlines the implementation plan for the Flight Control Center Demo Application, a simplified real-time flight operations dashboard that demonstrates stream processing concepts using Apache Flink with Kafka, integrated with Ktor to create an accessible demo.

## Project Structure

```
flink-ktor-table-api-json/
├── build.gradle.kts                  # Main Gradle build file using Kotlin DSL
├── settings.gradle.kts               # Gradle settings for multi-module project
├── docker/
│   └── docker-compose.yaml           # Docker Compose configuration (Kafka, PostgreSQL)
├── simulator/                        # Flight event generator module
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/demo/flight/simulator/
│           ├── FlightSimulator.kt    # Main simulator class
│           ├── EventGenerator.kt     # Flight event generation logic
│           └── models/               # Data models for flight events
├── processor/                        # Flink stream processing module
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/demo/flight/processor/
│           ├── jobs/                 # Flink job definitions
│           │   ├── DelayDetectionJob.kt
│           │   └── DensityAggregationJob.kt
│           ├── models/               # Data models for processing
│           └── connectors/           # Kafka and PostgreSQL connectors
├── api/                              # Ktor REST API module
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/demo/flight/api/
│           ├── Application.kt        # Main Ktor application
│           ├── routes/               # API route definitions
│           ├── models/               # Data models for API responses
│           └── services/             # Database interaction services
├── frontend/                         # Web Dashboard module
│   ├── package.json                  # npm package configuration
│   ├── webpack.config.js             # Webpack bundler configuration
│   ├── tsconfig.json                 # TypeScript configuration
│   ├── src/
│   │   ├── index.html               # Main HTML template
│   │   ├── index.ts                  # Application entry point
│   │   ├── components/               # UI components
│   │   │   ├── Map/                  # Hero Map visualization
│   │   │   ├── MetricsCard/          # Animated metrics cards
│   │   │   └── AlertFeed/            # Live alert notifications
│   │   ├── styles/                   # CSS styles with iOS/macOS aesthetic
│   │   ├── services/                 # API and WebSocket services
│   │   └── utils/                    # Utility functions
│   └── public/                       # Static assets
│       ├── images/                   # Image assets
│       └── fonts/                    # Font files
├── scripts/                          # Helper scripts for demo setup and execution
│   ├── setup.sh
│   └── demo.sh
└── Makefile                          # Makefile for common operations
```

## Technology Stack

- **Kotlin 1.9+**: Primary programming language
- **Gradle (Kotlin DSL)**: Build system
- **Apache Flink 2.0**: Stream processing framework
- **Kafka 3.5+**: Message broker (using Confluent images)
- **PostgreSQL 15**: Database for materialized views
- **Ktor 2.3+**: Web framework for REST API and WebSockets
- **Docker & Docker Compose**: Containerization

## Implementation Phases

### Phase 1: Project Setup

1. **Initialize Project Structure**
   - Create multi-module Gradle project with Kotlin DSL
   - Configure dependencies for all modules
   - Set up Docker Compose for Kafka and PostgreSQL

2. **Create Development Environment**
   - Configure Kafka topics
   - Set up PostgreSQL schemas and tables
   - Create Makefile with common operations

### Phase 2: Flight Event Simulator

1. **Implement Event Models**
   - Create `FlightEvent` data class with all required fields
   - Implement event type enum (`POSITION_UPDATE`, `DELAY_NOTIFICATION`, etc.)

2. **Develop Event Generator**
   - Implement realistic flight path generation
   - Create randomized delay scenarios
   - Configure Kafka producer for JSON serialization

3. **Build Simulator Application**
   - Implement command-line interface for simulator control
   - Create configurable simulation parameters

### Phase 3: Flink Stream Processing

1. **Implement Kafka Source Connector**
   - Configure Flink's Kafka connector for JSON messages
   - Set up deserialization for flight events

2. **Develop Delay Detection Job**
   - Implement stateless processing using Flink Table API
   - Create filter for detecting delayed flights
   - Configure PostgreSQL sink for delayed flight alerts

3. **Implement Density Aggregation Job**
   - Create windowed aggregation for flight density
   - Implement grid-based spatial aggregation
   - Configure PostgreSQL sink with upsert capability

4. **Set Up Job Deployment**
   - Configure Flink execution environment
   - Implement job submission logic

### Phase 4: Ktor REST API

1. **Create API Application Structure**
   - Set up Ktor server with required plugins
   - Configure JSON serialization

2. **Implement Database Service**
   - Create PostgreSQL connection pool
   - Implement data access methods for flight information

3. **Develop REST Endpoints**
   - Implement `/api/flights/density` endpoint
   - Create `/api/flights/delayed` endpoint
   - Develop additional informational endpoints

4. **Add WebSocket Support**
   - Implement `/api/flights/live` WebSocket endpoint
   - Create Kafka consumer for real-time updates

### Phase 5: Web Dashboard Implementation

1. **Dashboard Structure Setup**
   - Create frontend directory structure
   - Set up build system with npm/webpack
   - Configure static file serving in Ktor

2. **UI Component Development**
   - Implement premium iOS/macOS aesthetic design system
   - Create Hero Map Visualization with flight tracking
   - Develop Animated Metrics Cards with real-time updates
   - Build Live Alert Feed with iOS-style notifications
   - Implement dynamic light/dark mode support

3. **Real-time Data Integration**
   - Set up WebSocket connections for live data
   - Implement data transformation for visualization
   - Create smooth animations for data transitions
   - Add interactive filtering and controls

4. **Responsive Layout**
   - Implement responsive grid system
   - Create mobile-friendly views
   - Optimize for different screen sizes
   - Add touch support for mobile devices

### Phase 6: Integration and Testing

1. **End-to-End Testing**
   - Test data flow from simulator to dashboard
   - Verify real-time updates via WebSocket
   - Validate aggregation results
   - Test dashboard responsiveness and animations

2. **Performance Testing**
   - Test system with specified load (100+ events/second)
   - Measure and optimize response times
   - Optimize dashboard rendering performance
   - Test WebSocket connection stability

3. **Demo Preparation**
   - Create demo scripts for presentation
   - Prepare sample scenarios
   - Create guided tour of dashboard features

## Detailed Component Specifications

### Flight Event Schema

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

### Kafka Topics

1. **flight-events**: Raw flight events from simulator
2. **delayed-flights**: Processed delay notifications
3. **flight-density**: Aggregated flight density data

### PostgreSQL Tables

1. **current_flight_positions**
   - Primary key: `flight_id`
   - Columns: flight details, position, status

2. **delayed_flights**
   - Primary key: `flight_id`
   - Columns: flight details, delay information

3. **flight_density**
   - Primary key: `grid_lat`, `grid_lon`
   - Columns: grid coordinates, flight count, window timestamp

### Flink Table API Implementation

#### Delay Detection Job

```kotlin
// Using Flink Table API for delay detection
val tableEnv = StreamTableEnvironment.create(env)

// Register Kafka source
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
        WATERMARK FOR timestamp AS timestamp - INTERVAL '5' SECONDS
    ) WITH (
        'connector' = 'kafka',
        'topic' = 'flight-events',
        'properties.bootstrap.servers' = 'kafka:9092',
        'properties.group.id' = 'flink-delay-detector',
        'format' = 'json',
        'scan.startup.mode' = 'latest-offset'
    )
""")

// Register PostgreSQL sink
tableEnv.executeSql("""
    CREATE TABLE delayed_flights_sink (
        flightId STRING,
        airline STRING,
        delayMinutes INT,
        origin STRING,
        destination STRING,
        timestamp TIMESTAMP(3),
        PRIMARY KEY (flightId) NOT ENFORCED
    ) WITH (
        'connector' = 'jdbc',
        'url' = 'jdbc:postgresql://postgres:5432/flightdemo',
        'table-name' = 'delayed_flights',
        'username' = 'postgres',
        'password' = 'postgres',
        'sink.buffer-flush.max-rows' = '1',
        'sink.buffer-flush.interval' = '0'
    )
""")

// Process delays using Table API
val flightEvents = tableEnv.from("flight_events")

val delayedFlights = flightEvents
    .filter($("delayMinutes").isGreater(15))
    .select(
        $("flightId"),
        $("airline"),
        $("delayMinutes"),
        $("origin"),
        $("destination"),
        $("timestamp")
    )

// Execute the query
delayedFlights.executeInsert("delayed_flights_sink")
```

#### Density Aggregation Job

```kotlin
// Using Flink Table API for density aggregation
val tableEnv = StreamTableEnvironment.create(env)

// Register Kafka source (same as above)

// Register PostgreSQL sink for density
tableEnv.executeSql("""
    CREATE TABLE flight_density_sink (
        grid_lat INT,
        grid_lon INT,
        flight_count BIGINT,
        window_start TIMESTAMP(3),
        PRIMARY KEY (grid_lat, grid_lon) NOT ENFORCED
    ) WITH (
        'connector' = 'jdbc',
        'url' = 'jdbc:postgresql://postgres:5432/flightdemo',
        'table-name' = 'flight_density',
        'username' = 'postgres',
        'password' = 'postgres',
        'sink.buffer-flush.max-rows' = '1',
        'sink.buffer-flush.interval' = '0'
    )
""")

// Process density using Table API with windowing
val flightEvents = tableEnv.from("flight_events")

val flightDensity = flightEvents
    .filter($("eventType").isEqual("POSITION_UPDATE"))
    .window(Tumble.over(lit(1).minutes()).on($("timestamp")).as("w"))
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

// Execute the query
flightDensity.executeInsert("flight_density_sink")
```

### Ktor API Implementation

#### Application Configuration

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
                registerModule(JavaTimeModule())
            }
        }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
        }
        install(WebSockets)
        configureRouting()
    }.start(wait = true)
}
```

#### API Routes

```kotlin
fun Application.configureRouting() {
    val dbService = DatabaseService()
    
    routing {
        route("/api") {
            // Current flight density endpoint
            get("/flights/density") {
                val density = dbService.getCurrentDensity()
                call.respond(density)
            }
            
            // Delayed flights endpoint
            get("/flights/delayed") {
                val delayed = dbService.getDelayedFlights()
                call.respond(delayed)
            }
            
            // WebSocket for live updates
            webSocket("/flights/live") {
                val kafkaConsumer = createKafkaConsumer()
                kafkaConsumer.subscribe(listOf("flight-events"))
                
                try {
                    while (true) {
                        val records = kafkaConsumer.poll(Duration.ofMillis(100))
                        records.forEach { record ->
                            send(Frame.Text(record.value()))
                        }
                    }
                } finally {
                    kafkaConsumer.close()
                }
            }
        }
    }
}
```

## Docker Compose Configuration

```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.9.0
    ports:
      - "29092:29092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:29093
      KAFKA_LISTENERS: PLAINTEXT://kafka:9092,CONTROLLER://kafka:29093,PLAINTEXT_HOST://0.0.0.0:29092
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LOG_DIRS: /tmp/kraft-combined-logs
    volumes:
      - kafka-data:/var/lib/kafka/data

  schema-registry:
    image: confluentinc/cp-schema-registry:7.9.0
    depends_on:
      - kafka
    ports:
      - "8081:8081"
    environment:
      SCHEMA_REGISTRY_HOST_NAME: schema-registry
      SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: kafka:9092

  postgres:
    image: postgres:15
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: flightdemo
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./postgres/init.sql:/docker-entrypoint-initdb.d/init.sql

volumes:
  kafka-data:
  postgres-data:
```

## Makefile

```makefile
# Colors and emojis for better readability
BLUE=\033[0;34m
GREEN=\033[0;32m
YELLOW=\033[0;33m
RED=\033[0;31m
NC=\033[0m # No Color

.PHONY: setup start stop clean build run-simulator run-processor run-api demo status help

# Setup development environment
setup: ## 🚀 Setup development environment
	@echo "${BLUE}🚀 Setting up flight demo environment...${NC}"
	@docker-compose -f docker/docker-compose.yaml up -d
	@echo "${YELLOW}⏳ Waiting for services to be ready...${NC}"
	@sleep 10
	@echo "${GREEN}✅ Environment setup complete!${NC}"

# Start all services
start: ## 🚀 Start all services
	@echo "${BLUE}🚀 Starting all services...${NC}"
	@docker-compose -f docker/docker-compose.yaml up -d
	@echo "${GREEN}✅ Services started!${NC}"

# Stop all services
stop: ## 🛑 Stop all services
	@echo "${BLUE}🛑 Stopping all services...${NC}"
	@docker-compose -f docker/docker-compose.yaml down
	@echo "${GREEN}✅ Services stopped!${NC}"

# Clean environment
clean: ## 🧹 Clean environment
	@echo "${BLUE}🧹 Cleaning environment...${NC}"
	@docker-compose -f docker/docker-compose.yaml down -v
	@echo "${GREEN}✅ Environment cleaned!${NC}"

# Build all modules
build: ## 🔨 Build all modules
	@echo "${BLUE}🔨 Building all modules...${NC}"
	@./gradlew clean build
	@echo "${GREEN}✅ Build complete!${NC}"

# Run flight simulator
run-simulator: ## ✈️ Run flight simulator
	@echo "${BLUE}✈️ Running flight simulator...${NC}"
	@./gradlew :simulator:run
	@echo "${GREEN}✅ Simulator started!${NC}"

# Run Flink processor
run-processor: ## 🔄 Run Flink processor
	@echo "${BLUE}🔄 Running Flink processor...${NC}"
	@./gradlew :processor:run
	@echo "${GREEN}✅ Processor started!${NC}"

# Run Ktor API
run-api: ## 🌐 Run Ktor API
	@echo "${BLUE}🌐 Running Ktor API...${NC}"
	@./gradlew :api:run
	@echo "${GREEN}✅ API started!${NC}"

# Run demo
demo: ## 🎮 Run complete demo
	@echo "${BLUE}🎮 Running complete demo...${NC}"
	@echo "${YELLOW}Step 1: Starting services...${NC}"
	@make start
	@echo "${YELLOW}Step 2: Running processor...${NC}"
	@./gradlew :processor:run &
	@echo "${YELLOW}Step 3: Running API...${NC}"
	@./gradlew :api:run &
	@echo "${YELLOW}Step 4: Running simulator...${NC}"
	@./gradlew :simulator:run
	@echo "${GREEN}✅ Demo running!${NC}"

# Show status
status: ## 📊 Show status of all components
	@echo "${BLUE}📊 Demo Component Status:${NC}"
	@echo "${YELLOW}Docker Services:${NC}"
	@docker-compose -f docker/docker-compose.yaml ps
	@echo "${GREEN}✅ Status check complete!${NC}"

# Help
help: ## 📚 Show this help
	@echo "${BLUE}📚 Available commands:${NC}"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "${YELLOW}%-20s${NC} %s\n", $$1, $$2}'

# Default target
.DEFAULT_GOAL := help
```

## Implementation Timeline

1. **Week 1: Project Setup & Simulator Development**
   - Day 1-2: Project structure, Docker setup
   - Day 3-5: Flight event simulator implementation

2. **Week 2: Flink Processing Implementation**
   - Day 1-3: Delay detection job
   - Day 4-5: Density aggregation job

3. **Week 3: Ktor API Development & Integration**
   - Day 1-3: REST API implementation
   - Day 4-5: WebSocket support and integration testing

4. **Week 4: Testing, Optimization & Demo Preparation**
   - Day 1-3: Performance testing and optimization
   - Day 4-5: Demo script preparation and documentation

## Success Criteria Validation

The implementation will be considered successful when:

1. **Functional Requirements**
   - Flight events are generated and processed in real-time
   - Delayed flights are detected and reported
   - Flight density is calculated and visualized
   - All data is accessible via REST API and WebSockets

2. **Performance Requirements**
   - System handles 100+ flight events per second
   - API response times are under 1 second
   - End-to-end latency (from event generation to API response) is under 5 seconds

3. **Operational Requirements**
   - System can be started with a single command
   - Demo scenarios can be executed easily
   - All components are properly containerized

## Conclusion

This implementation plan provides a comprehensive roadmap for developing the Flight Control Center Demo Application. By following this plan, we will create a simplified yet powerful demonstration of stream processing concepts using Apache Flink, Kafka, and Ktor, all working together to provide real-time flight information.
