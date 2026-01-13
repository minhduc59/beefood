# Dockerfile for Discovery Service
FROM maven:latest
# VOLUME instruction to define a mount point with the specified path and marks it as holding externally mounted volumes from native host or other containers.
VOLUME /tmp
# Define build argument for project version
ARG PROJECT_VERSION=0.0.1
# Create application directory
RUN mkdir -p /home/app
# Set working directory
WORKDIR /home/app
# Set active Spring profile
ENV SPRING_PROFILES_ACTIVE application
# Copy project files to the container
COPY ./ .
# Build the project using Maven
ADD target/discovery-service-0.0.1-SNAPSHOT.jar discovery-service-0.0.1-SNAPSHOT.jar
# Expose Eureka server port, other services will communicate through this port
EXPOSE 8761
# Run the application
ENTRYPOINT ["java", "-jar", "discovery-service-0.0.1-SNAPSHOT.jar"]
# Alternatively, you can use Maven to run the application
CMD ["mvn", "spring-boot:run"]