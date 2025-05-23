# Flight Control Center Demo Application

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-blue.svg)](https://kotlinlang.org/)
[![Gradle](https://img.shields.io/badge/Gradle-Kotlin%20DSL-green.svg)](https://gradle.org/)
[![Flink](https://img.shields.io/badge/Apache%20Flink-2.0.0-red.svg)](https://flink.apache.org/)
[![Kafka](https://img.shields.io/badge/Kafka-7.9.0%20%7C%204.0.0-black.svg)](https://kafka.apache.org/)
[![Ktor](https://img.shields.io/badge/Ktor-2.3.7-purple.svg)](https://ktor.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![JVM](https://img.shields.io/badge/JVM-21-orange.svg)](https://openjdk.java.net/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)

A simplified real-time flight operations dashboard that demonstrates stream processing concepts using Apache Flink with Kafka, integrated with Ktor to create an accessible demo.

## Project Overview

This application showcases Apache Flink's stream processing capabilities with Kafka using simple JSON messages, integrated with Ktor to create a real-time flight operations dashboard. The system processes flight events to detect delays and calculate flight density in real-time.

## Architecture

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

## Technology Stack

- **Kotlin 1.9+**: Primary programming language
- **Gradle (Kotlin DSL)**: Build system
- **Apache Flink 2.0**: Stream processing framework
- **Kafka 3.5+**: Message broker (using Confluent images)
- **PostgreSQL 15**: Database for materialized views
- **Ktor 2.3+**: Web framework for REST API and WebSockets
- **Docker & Docker Compose**: Containerization

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
│           │   ├── FlinkJobBase.kt         # Base class for Flink jobs
│           │   ├── FlightProcessingJobs.kt # Main entry point
│           │   ├── DelayDetectionJob.kt    # Detects delayed flights
│           │   └── DensityAggregationJob.kt # Calculates flight density
│           ├── models/               # Data models for processing
│           │   └── FlightEvent.kt          # Flight event data model
│           └── connectors/           # Kafka and PostgreSQL connectors
│               ├── KafkaSourceConnector.kt # Kafka source implementation
│               ├── JdbcSinkConnector.kt    # PostgreSQL sink implementation
│               └── FlightEventDeserializationSchema.kt # JSON deserialization
├── api/                              # Ktor REST API module
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/demo/flight/api/
│           ├── Application.kt        # Main Ktor application
│           ├── routes/               # API route definitions
│           ├── models/               # Data models for API responses
│           └── services/             # Database interaction services
├── frontend/                         # Web Dashboard module
│   ├── package.json
│   └── src/
│       └── index.html
└── Makefile                          # Makefile for common operations
```

## Getting Started

### Prerequisites

- Docker and Docker Compose
- JDK 17 or higher
- Gradle 8.0 or higher
- Node.js and npm (for the dashboard)

### Setup

1. Clone the repository
2. Run the setup command:

```bash
make setup
```

This will start the required Docker containers (Kafka, PostgreSQL) and initialize the database.

## Flink Stream Processing

The processor module contains two Flink jobs:

1. **DelayDetectionJob**: Processes flight events to detect delayed flights and stores them in PostgreSQL.
2. **DensityAggregationJob**: Calculates flight density by geographic grid and stores the results in PostgreSQL.

### Running the Flink Jobs

To run the Flink jobs, use the following commands:

```bash
# Run both jobs
./gradlew :processor:run

# Run only the delay detection job
./gradlew :processor:run --args="delay"

# Run only the density aggregation job
./gradlew :processor:run --args="density"
```

### Flink Job Architecture

The Flink jobs use the DataStream API to process flight events from Kafka and store the results in PostgreSQL:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  Kafka Source   │───▶│ DataStream API   │───▶│   JDBC Sink     │
│  (Flight Events)│    │ (Transformations)│    │  (PostgreSQL)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
```

The implementation uses:
- Kafka source connector for consuming flight events
- JDBC sink connector for writing to PostgreSQL
- Custom deserialization for JSON messages
- Event time processing with watermarks

### Running the Application

To run the complete demo:

```bash
make demo
```

Or run each component separately:

```bash
# Start the infrastructure
make start

# Run the Flink processor
make run-processor

# Run the Ktor API
make run-api

# Run the flight simulator
make run-simulator

# Run the frontend dashboard
make run-frontend
```

### Dashboard Access

Once all components are running, access the dashboard at:

```
http://localhost:9000
```

### Environment Configuration

The frontend dashboard can be configured using environment variables. Create a `.env` file in the `frontend` directory with the following variables:

```
# API Configuration
REACT_APP_API_BASE_URL=http://localhost:8090
REACT_APP_WS_FLIGHTS_LIVE=/api/flights/live
REACT_APP_API_FLIGHTS_DENSITY=/api/flights/density
REACT_APP_API_FLIGHTS_DELAYED=/api/flights/delayed

# Dashboard Configuration
REACT_APP_UPDATE_INTERVAL=1000
REACT_APP_MAX_ALERTS=20
REACT_APP_MAP_CENTER_LAT=50.0
REACT_APP_MAP_CENTER_LNG=10.0
REACT_APP_MAP_ZOOM=5
```

You can also copy the provided `.env.example` file as a starting point:

```bash
cp frontend/.env.example frontend/.env
```

### API Endpoints

- `GET /api/flights/density` - Get current flight density data
- `GET /api/flights/delayed` - Get delayed flights information
- `WebSocket /api/flights/live` - Real-time flight updates

## Development

### Building the Project

```bash
make build
```

### Checking Status

```bash
make status
```

### Cleaning Up

```bash
make clean
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
