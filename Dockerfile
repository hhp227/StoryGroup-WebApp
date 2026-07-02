# syntax=docker/dockerfile:1

# --- Build stage: Gradle 6.8.3은 JDK 15까지만 지원하므로 JDK 11로 고정 ---
FROM eclipse-temurin:11-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

# --- Run stage ---
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Cloud Run이 주입하는 PORT 환경변수를 Spring Boot가 사용 (application.properties: server.port=${PORT:8080})
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
