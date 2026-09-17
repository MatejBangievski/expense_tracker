# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the Spring Boot fat jar ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the Gradle wrapper + build scripts first so dependency resolution
# is cached as long as these files don't change.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

# Then the sources.
COPY src ./src

# Build the executable jar. Tests run in CI (they need Docker for Testcontainers),
# so we skip them here to keep the image build fast and hermetic.
RUN ./gradlew --no-daemon clean bootJar -x test

# ---- Runtime stage: small JRE image, non-root ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system app && useradd --system --gid app app

# Copy only the executable boot jar (the *-plain.jar is excluded by the glob).
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
