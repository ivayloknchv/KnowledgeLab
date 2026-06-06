# Stage 1: Build
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /workspace

# Cache Gradle dependencies separately from source
COPY build.gradle settings.gradle gradle.properties* ./
COPY gradle gradle
COPY gradlew .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q

COPY src src
RUN ./gradlew bootJar --no-daemon -q

# Extract layers for optimal Docker layer caching
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination build/extracted

# Stage 2: Runtime
FROM eclipse-temurin:25-jre-alpine AS runtime

# Security: run as non-root
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

WORKDIR /app

# Copy layers in order of change frequency (least → most)
COPY --from=builder /workspace/build/extracted/dependencies/          ./
COPY --from=builder /workspace/build/extracted/spring-boot-loader/    ./
COPY --from=builder /workspace/build/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/build/extracted/application/           ./

USER appuser

EXPOSE 8080

# Use JVM flags optimized for containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]