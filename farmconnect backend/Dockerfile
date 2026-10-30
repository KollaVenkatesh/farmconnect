# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build --chown=spring:spring /app/target/ecommerce-backend-0.0.1-SNAPSHOT.jar app.jar
USER spring:spring
EXPOSE 10000
CMD ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar"]
