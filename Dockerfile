# Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Runtime (Render-friendly: honors $PORT)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Optional: Node for Cursor SDK bridge (SHAPE fallback works without npm SDK install)
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl ca-certificates \
  && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
  && apt-get install -y --no-install-recommends nodejs \
  && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/rest-assured-framework-1.0-SNAPSHOT.jar app.jar
COPY ai-bridge ./ai-bridge

# Install Cursor SDK when NPM token available at build (optional)
# docker build --build-arg NPM_TOKEN=... 
ARG NPM_TOKEN=
RUN if [ -n "$NPM_TOKEN" ]; then \
      cd /app/ai-bridge && npm config set //registry.npmjs.org/:_authToken="$NPM_TOKEN" \
      && npm install --omit=dev || true; \
    fi

ENV PORT=8080
EXPOSE 8080

# Render sets PORT dynamically — do not hardcode 8080 only
ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT:-8080}"]
