# Flight Control Center Demo Application

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
└── Makefile                          # Makefile for common operations
```

## Getting Started

### Prerequisites

- Docker and Docker Compose
- JDK 17 or higher
- Gradle 8.0 or higher

### Setup

1. Clone the repository
2. Run the setup command:

```bash
make setup
```

This will start the required Docker containers (Kafka, PostgreSQL) and initialize the database.

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
