# ─────────────── Stage 1: Build ───────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ─────────────── Stage 2: Runtime ───────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q --spider http://localhost:8088/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_TOOL_OPTIONS -jar /app/app.jar"]
