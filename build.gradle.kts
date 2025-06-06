import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0" apply false
    kotlin("plugin.serialization") version "2.1.21" apply false
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

        // Kotlin coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

        // Kafka
        implementation("org.apache.kafka:kafka-clients:3.5.1")

        // Jackson for JSON serialization
        implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

        // PostgreSQL
        implementation("org.postgresql:postgresql:42.6.0")
        implementation("com.zaxxer:HikariCP:5.0.1")

        // Logging
        implementation("org.slf4j:slf4j-api:2.0.9")
        implementation("ch.qos.logback:logback-classic:1.4.11")
        implementation("ch.qos.logback:logback-core:1.4.11")
        implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")

        // Testing
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
        testImplementation("io.mockk:mockk:1.13.9")
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
