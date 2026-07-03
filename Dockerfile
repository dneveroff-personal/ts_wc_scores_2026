FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Jar переименовывается в app.jar при деплое (см. Makefile)
# Стабильное имя + --no-cache гарантируют что Docker не использует старый слой
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx256m", "-jar", "app.jar"]
