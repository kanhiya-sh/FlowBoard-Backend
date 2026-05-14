# ─────────────── Stage 1: Build ───────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache Maven dependencies first (better layer reuse)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─────────────── Stage 2: Runtime ───────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8761

# Health check via Spring Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8761/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]
