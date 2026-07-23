# Stage 1: Build the React Frontend
FROM node:22-alpine AS frontend-build
WORKDIR /app

# Copy package descriptors and install dependencies using clean install
COPY frontend/package*.json ./
RUN npm ci

# Copy the rest of the frontend source and build the production bundle
COPY frontend/ ./
ENV VITE_API_BASE_URL=""
RUN npm run build

# Stage 2: Build the Java Spring Boot Backend
FROM maven:3.9-eclipse-temurin-21-jammy AS backend-build
WORKDIR /app

# Copy the pom.xml and cache Maven dependencies offline
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy the backend source code
COPY backend/ .

# Inject the compiled frontend assets from Stage 1 into Spring Boot's static resources
COPY --from=frontend-build /app/dist/ ./src/main/resources/static/

# Build the final self-contained executable JAR
RUN mvn clean package -DskipTests

# Stage 3: Create the final production runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Node.js 22 LTS and npm (required for JHipster generator invocation)
RUN apt-get update && \
    apt-get install -y curl git && \
    curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && \
    apt-get install -y nodejs && \
    git --version && \
    node -v && \
    npm -v && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy the unified executable JAR file from Stage 2
COPY --from=backend-build /app/target/*.jar app.jar

# Copy the custom JHipster generator fork
COPY generator-jhipster /app/generator-jhipster

# Install JHipster dependencies, build the TypeScript distribution, and prune devDependencies
RUN cd /app/generator-jhipster && \
    npm ci && \
    npm prune --production

# Set environment variables for the generator fork
ENV JHIPSTER_FORK_PATH=/app/generator-jhipster

# Expose the standard port
EXPOSE 8080

# Start the unified Spring Boot and React application
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-jar", "app.jar"]