# Stage 1: Build application using Maven
FROM maven:3.8.5-openjdk-17-slim AS builder
WORKDIR /app

# Copy pom.xml and download dependencies to cache them
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the package
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Runtime environment
FROM openjdk:17-slim
WORKDIR /app

# Create a non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy built WAR file from the builder stage
# (The artifact is packaged as a war, but Spring Boot makes it executable directly)
COPY --from=builder /app/target/DuAnBanSach-0.0.1-SNAPSHOT.war app.war

# Expose port
EXPOSE 8080

# Run Spring Boot application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.war"]
