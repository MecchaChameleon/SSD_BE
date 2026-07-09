# ---- Build stage ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /app

# 의존성 캐시를 위해 빌드 스크립트 먼저 복사
COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle clean bootJar --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
