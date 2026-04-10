# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY habit-tracker-backend/pom.xml .
COPY habit-tracker-backend/src ./src
RUN echo "Starting Maven build..." && \
    mvn clean package -DskipTests && \
    echo "Build complete, listing target:" && \
    ls -la target/ && \
    if [ ! -f target/zenko-backend-1.0.0.jar ]; then echo "ERROR: JAR not found!"; exit 1; fi

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy jar from builder with error checking
COPY --from=builder /app/target/zenko-backend-1.0.0.jar app.jar
RUN echo "JAR copied successfully" && ls -lh app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run with production profile by default
ENV SPRING_PROFILES_ACTIVE=prod
CMD ["java", "-jar", "app.jar"]
