# ═══════════════════════════════════════════════════════════════════════════
# Multi-stage Dockerfile for Hyper Backend - Optimized for Render Deployment
# Includes optimizations for invoice generation (async processing, PDF generation)
# ═══════════════════════════════════════════════════════════════════════════

# ═══════════════════════════════════════════════════════════════════════════
# BUILD STAGE - Create production JAR
# ═══════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Install build dependencies
RUN apk add --no-cache dos2unix

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Fix line endings and make mvnw executable
RUN dos2unix mvnw && chmod +x mvnw

# Download dependencies (cached layer - only re-runs if pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./mvnw package -DskipTests

# ═══════════════════════════════════════════════════════════════════════════
# RUNTIME STAGE - Optimized JRE with production configurations
# ═══════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine

# Install runtime dependencies for PDF generation
# - fontconfig: Font handling for PDF generation
# - ttf-dejavu: Unicode fonts for invoice PDFs
RUN apk add --no-cache \
    fontconfig \
    ttf-dejavu

# Create non-root user for security
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/Hyper_backend-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Set default Spring profile to staging
ENV SPRING_PROFILES_ACTIVE=staging

# Expose port (Render will override with $PORT environment variable)
EXPOSE 8080

# ═══════════════════════════════════════════════════════════════════════════
# JVM Optimization Flags for Production
# ═══════════════════════════════════════════════════════════════════════════
# Memory Settings (Render Free Tier: 512MB RAM):
# - Xms256m: Initial heap size (50% of available memory)
# - Xmx400m: Maximum heap size (80% of available memory, leave room for native memory)
# - XX:MaxMetaspaceSize=128m: Metaspace limit (class metadata)
# - XX:ReservedCodeCacheSize=64m: Code cache for JIT compiler
#
# Garbage Collection:
# - XX:+UseG1GC: G1 collector (good for low-pause times)
# - XX:MaxGCPauseMillis=200: Target max pause time for GC
# - XX:G1HeapRegionSize=1M: Region size for G1
#
# Performance:
# - XX:+UseStringDeduplication: Reduce memory for duplicate strings
# - XX:+OptimizeStringConcat: Optimize string concatenation
#
# Async Invoice Generation:
# - XX:+UseContainerSupport: Respect container memory limits
# - XX:ActiveProcessorCount=2: Limit async thread pool size
# ═══════════════════════════════════════════════════════════════════════════

ENTRYPOINT ["sh", "-c", "\
    # Convert Render's DATABASE_URL format (postgres://) to JDBC format (jdbc:postgresql://) \
    if [ -n \"$DATABASE_URL\" ] && echo \"$DATABASE_URL\" | grep -q '^postgres://'; then \
        export DATABASE_URL=$(echo $DATABASE_URL | sed 's/^postgres:/jdbc:postgresql:/'); \
    fi && \
    # Run application with optimized JVM settings \
    exec java \
        -Xms256m \
        -Xmx400m \
        -XX:MaxMetaspaceSize=128m \
        -XX:ReservedCodeCacheSize=64m \
        -XX:+UseG1GC \
        -XX:MaxGCPauseMillis=200 \
        -XX:G1HeapRegionSize=1M \
        -XX:+UseStringDeduplication \
        -XX:+OptimizeStringConcat \
        -XX:+UseContainerSupport \
        -XX:ActiveProcessorCount=2 \
        -Djava.security.egd=file:/dev/./urandom \
        -Dfile.encoding=UTF-8 \
        -jar app.jar"]

# ═══════════════════════════════════════════════════════════════════════════
# Health Check Configuration
# ═══════════════════════════════════════════════════════════════════════════
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
