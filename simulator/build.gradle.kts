plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    // Kafka
    implementation("org.apache.kafka:kafka-clients:3.5.1")
    
    // JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    
    // Command line parsing
    implementation("com.github.ajalt.clikt:clikt:4.2.0")
    
    // Faker for generating realistic data
    implementation("com.github.javafaker:javafaker:1.0.2")
}

application {
    mainClass.set("com.demo.flight.simulator.FlightSimulatorKt")
}
