import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
}

group = "com.tswcscores"
version = "1.0.2"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

val telegramBotsVersion = "6.9.7.1"

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // PostgreSQL + Flyway
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JAXB (required for Java 11+ where it was removed from JDK)
    // javax.xml.bind API (bridge for old annotations used by telegrambots/jackson)
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    // jakarta.xml.bind runtime (for Hibernate 6.x)
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.3")

    // Telegram Bots
    implementation("org.telegram:telegrambots:$telegramBotsVersion")

    // Lombok (для Java-классов, если останутся)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Dotenv for local development
    implementation("me.paulschwarz:spring-dotenv:4.0.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Только один jar на выходе — executable bootJar
tasks.named("jar") {
    enabled = false
}
