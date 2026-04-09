# Multi-stage build for Zenko Backend

# Stage 1: Build the application
FROM maven:3.9.0-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY habit-tracker-backend/pom.xml .
RUN mvn dependency:resolve

# Copy source code
COPY habit-tracker-backend/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/zenko-backend-1.0.0.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/habits || exit 1

# Expose port
EXPOSE 8080

# Set environment
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
