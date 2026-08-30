# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY contracts contracts
COPY src src

RUN ./mvnw -B --no-transfer-progress package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk upgrade --no-cache libcrypto3 libssl3 openssl \
    && addgroup -S nitrogen \
    && adduser -S -G nitrogen nitrogen

COPY --from=build /workspace/target/nitrogen-backend-*.jar /app/nitrogen-backend.jar

ENV JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=web

USER nitrogen:nitrogen
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/nitrogen-backend.jar"]
