# ---- Build stage: compile and package the Spring Boot fat jar ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage: small JRE image + the finished jar ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
