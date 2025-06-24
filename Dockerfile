# syntax=docker/dockerfile:1

# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS build
LABEL maintainer="engineering immigrant"
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Run Stage ----
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="engineering immigrant"
VOLUME /tmp
WORKDIR /app
COPY --from=build /app/target/devspace-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"] 