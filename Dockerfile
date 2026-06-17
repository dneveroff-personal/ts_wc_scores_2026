FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Копируем предварительно собранный jar (plain jar отключён в gradle)
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
