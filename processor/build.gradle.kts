plugins {
    kotlin("jvm")
    application
}

dependencies {
    // Apache Flink
    implementation("org.apache.flink:flink-streaming-java:2.0.0")
    implementation("org.apache.flink:flink-clients:2.0.0")
    implementation("org.apache.flink:flink-table-api-java-bridge:2.0.0")
    implementation("org.apache.flink:flink-table-planner-loader:2.0.0")
    implementation("org.apache.flink:flink-runtime-web:2.0.0")
    
    // Flink connectors
    implementation("org.apache.flink:flink-connector-kafka:4.0.0-2.0")
    implementation("org.apache.flink:flink-connector-jdbc-core:4.0.0-2.0")
    
    // JSON processing
    implementation("org.apache.flink:flink-json:2.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
    
    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.6.0")
}

application {
    mainClass.set("com.demo.flight.processor.jobs.FlightProcessingJobsKt")
}
