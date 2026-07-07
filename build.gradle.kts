import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
    id("com.diffplug.spotless") version "6.25.0"
    jacoco
}

group = "com.tswcscores"
version = "5.3.6"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // H2 + Flyway
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.flywaydb:flyway-core")

    // OkHttp — для своего Telegram HTTP клиента
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

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

springBoot {
    buildInfo {
        properties {
            version = project.version.toString()
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

// Только один jar на выходе — executable bootJar
tasks.named("jar") {
    enabled = false
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

spotless {
    java {
        googleJavaFormat("1.21.0")
            .reflowLongStrings()
            .skipJavadocFormatting()

        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()

        targetExclude("build/**", "**/generated/**")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Чтобы spotlessCheck запускался перед компиляцией
tasks.compileJava {
    dependsOn(tasks.spotlessCheck)
}

// ### SpotlessCheck ###
//# Проверка стиля
//        ./gradlew spotlessCheck
//
//# Авто-исправление всех ошибок стиля (магия! ✨)
//./gradlew spotlessApply
//
//# Только для Checkstyle
//    ./gradlew checkstyleMain
//    ./gradlew checkstyleTest
