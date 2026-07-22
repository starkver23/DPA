# Stage 1: Build the Java application
FROM maven:3.9-eclipse-temurin-21-jammy AS build
WORKDIR /app

# Copy the pom.xml and download dependencies for offline caching
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY backend/ .
RUN mvn clean package -DskipTests

# Stage 2: Create the production runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Node.js 22 LTS and npm
RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y nodejs && \
    node -v && npm -v && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy the compiled JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy the modified JHipster generator
COPY generator-jhipster /app/generator-jhipster

# Set the environment variable for the JHipster generator fork
ENV JHIPSTER_FORK_PATH=/app/generator-jhipster

# Expose the application port
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]