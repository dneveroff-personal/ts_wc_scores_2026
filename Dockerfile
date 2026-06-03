FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Кэшируем зависимости отдельным слоем
COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew dependencies --no-daemon -q

COPY src ./src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
