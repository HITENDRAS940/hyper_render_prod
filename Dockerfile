# ═══════════════════════════════════════════════════════════════════════════
# Multi-stage Dockerfile for Hyper Backend - Memory Optimized for Render Free Tier (512MB)
# ═══════════════════════════════════════════════════════════════════════════

# ═══════════════════════════════════════════════════════════════════════════
# BUILD STAGE - Create production JAR with optimizations
# ═══════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml (minimal setup)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable (no dos2unix needed)
RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build with memory optimization and skip tests
RUN ./mvnw clean package -DskipTests \
    -Dmaven.compiler.debug=false \
    -Dmaven.compiler.debuglevel=none

# ═══════════════════════════════════════════════════════════════════════════
# RUNTIME STAGE - Memory Optimized JRE for 512MB Limit
# ═══════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine

# Install only essential runtime dependencies for PDF generation
# Keep font package minimal to save memory
RUN apk add --no-cache \
    fontconfig \
    ttf-dejavu \
    && rm -rf /var/cache/apk/*

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

# Expose port
EXPOSE 8080

# ═══════════════════════════════════════════════════════════════════════════
# JVM Optimization Flags for 512MB RAM Limit (Render Free Tier)
# ═══════════════════════════════════════════════════════════════════════════
# Memory Allocation Strategy (OPTIMIZED):
# - Total Available: 512MB
# - JVM Heap: 280MB max (Xmx) - INCREASED for stability
# - Metaspace: 80MB max - REDUCED (no invoice generation = fewer classes)
# - Code Cache: 24MB - REDUCED
# - Thread Stacks: ~40MB (optimized thread count)
# - Native Memory + Overhead: ~88MB
# - G1GC used for better throughput and concurrent processing
# ═══════════════════════════════════════════════════════════════════════════

ENTRYPOINT ["sh", "-c", "\
    # Convert Render's DATABASE_URL format \
    if [ -n \"$DATABASE_URL\" ] && echo \"$DATABASE_URL\" | grep -q '^postgres://'; then \
        export DATABASE_URL=$(echo $DATABASE_URL | sed 's/^postgres:/jdbc:postgresql:/'); \
    fi && \
    # Run with optimized memory settings for concurrent tasks \
    exec java \
        -Xms160m \
        -Xmx280m \
        -XX:MaxMetaspaceSize=80m \
        -XX:MetaspaceSize=64m \
        -XX:ReservedCodeCacheSize=24m \
        -XX:MaxDirectMemorySize=24m \
        -XX:+UseG1GC \
        -XX:MaxGCPauseMillis=200 \
        -XX:G1HeapRegionSize=1m \
        -XX:InitiatingHeapOccupancyPercent=70 \
        -XX:+UseContainerSupport \
        -XX:ActiveProcessorCount=2 \
        -XX:+TieredCompilation \
        -XX:TieredStopAtLevel=2 \
        -XX:+UseCompressedOops \
        -XX:+UseCompressedClassPointers \
        -XX:+ExitOnOutOfMemoryError \
        -XX:+HeapDumpOnOutOfMemoryError \
        -XX:HeapDumpPath=/tmp/heapdump.hprof \
        -XX:+UseStringDeduplication \
        -XX:StringTableSize=10000 \
        -Djava.security.egd=file:/dev/./urandom \
        -Dfile.encoding=UTF-8 \
        -Djava.awt.headless=true \
        -jar app.jar"]

# ═══════════════════════════════════════════════════════════════════════════
# Health Check Configuration
# ═══════════════════════════════════════════════════════════════════════════
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
