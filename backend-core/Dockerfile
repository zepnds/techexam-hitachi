# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy parent pom and module files
COPY pom.xml .
COPY backend-common ./backend-common
COPY backend-core ./backend-core
COPY backend-gateway ./backend-gateway

# Build core artifact
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -pl backend-core -am

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S notifgroup && adduser -S notifuser -G notifgroup

COPY --from=builder /workspace/backend-core/target/backend-core-1.0.0.jar app.jar

USER notifuser

EXPOSE 8081

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
