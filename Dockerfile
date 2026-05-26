# ---- Build Stage ----
FROM gradle:8.14-jdk21 AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle

COPY bootstrap/build.gradle.kts bootstrap/
COPY http/build.gradle.kts http/
COPY persistence/build.gradle.kts persistence/
COPY external/build.gradle.kts external/
COPY batch/build.gradle.kts batch/
COPY core/build.gradle.kts core/
COPY common/build.gradle.kts common/

COPY . .
RUN gradle :bootstrap:bootJar --no-daemon -x test

# ---- Run Stage ----
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/bootstrap/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]