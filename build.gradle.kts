import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.serialization") version "1.9.25" apply false
    id("io.ktor.plugin") version "2.3.13" apply false
}

allprojects {
    group = "com.demo.flight"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://packages.confluent.io/maven/") }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    dependencies {
        val implementation by configurations
        val testImplementation by configurations
        
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        
        // Kafka
        implementation("org.apache.kafka:kafka-clients:3.9.1")
        
        // Jackson for JSON serialization
        implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")
        
        // PostgreSQL
        implementation("org.postgresql:postgresql:42.6.0")
        implementation("com.zaxxer:HikariCP:5.1.0")
        
        // Logging
        implementation("org.slf4j:slf4j-api:2.0.17")
        implementation("ch.qos.logback:logback-classic:1.4.11")
        implementation("ch.qos.logback:logback-core:1.4.11")
        implementation("io.github.oshai:kotlin-logging-jvm:5.1.4")
        
        // Testing
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
        testImplementation("io.mockk:mockk:1.14.2")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<JavaCompile> {
        targetCompatibility = "21"
        sourceCompatibility = "21"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// Common dependency versions that will be used across modules
object Versions {
    const val flink = "2.0.0"
    const val kafka = "4.0.0"
    const val ktor = "2.3.7"
    const val jackson = "2.15.2"
    const val postgresql = "42.6.0"
    const val hikaricp = "5.0.1"
}
