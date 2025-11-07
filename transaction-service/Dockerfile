# Simple Dockerfile - Build locally first, then copy JAR
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the pre-built JAR file
# Make sure to run 'mvn clean package' BEFORE building Docker image
COPY transaction-service/target/*.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]