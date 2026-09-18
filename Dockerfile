# Multi-stage Dockerfile for Goliath-License-API

# Builder stage: use a JDK to run the Gradle wrapper and produce the bootJar
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

# Copy project files (use a narrow set to leverage Docker cache in real CI; copying all for simplicity here)
COPY . /workspace

# Ensure the Gradle wrapper is executable and build the bootJar
RUN chmod +x ./gradlew \
    && ./gradlew --no-daemon clean bootJar --warning-mode=none

# Runtime stage: use a small JRE image
FROM eclipse-temurin:21-jre-jammy

# Create non-root user
RUN addgroup --system app && adduser --system --ingroup app app

WORKDIR /app

# Copy jar from builder output
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar
RUN chown app:app /app/app.jar

# Switch to non-root user
USER app

# Let Render set PORT; fallback to 8080 if not provided
ENV JAVA_OPTS=""

EXPOSE 8080

# Start Spring Boot, honoring PORT env var (Render provides PORT). Uses shell form for ${PORT:-8080} expansion.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/urandom -jar /app/app.jar --server.port=${PORT:-8080}"]
