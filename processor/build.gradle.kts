plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

dependencies {
    // Kotlin logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    
    // Apache Flink Core
    implementation("org.apache.flink:flink-streaming-java:2.0.0")
    implementation("org.apache.flink:flink-clients:2.0.0")
    implementation("org.apache.flink:flink-runtime-web:2.0.0")
    
    // Flink Table API
    implementation("org.apache.flink:flink-table-api-java-bridge:2.0.0")
    implementation("org.apache.flink:flink-table-planner-loader:2.0.0")
    implementation("org.apache.flink:flink-table-runtime:2.0.0")
    implementation("org.apache.flink:flink-table-common:2.0.0")
    
    // Flink SQL format
    implementation("org.apache.flink:flink-json:2.0.0")
    
    // Flink connectors
    implementation("org.apache.flink:flink-connector-kafka:4.0.0-2.0")
    
    implementation("org.apache.flink:flink-connector-jdbc-core:4.0.0-2.0")
    implementation("org.apache.flink:flink-connector-jdbc-postgres:4.0.0-2.0")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")
    
    // PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.5")
    
    // Testing dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    // For Kotlin object with @JvmStatic main method, we don't need the Kt suffix
    mainClass.set("com.demo.flight.processor.jobs.FlightProcessingJobs")
}
