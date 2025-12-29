# Dockerfile for Restaurant Service
FROM maven:latest
VOLUME /tmp
ARG PROJECT_VERSION=0.0.1
RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE application
COPY ./ .
ADD target/restaurant-service-0.0.1-SNAPSHOT.jar restaurant-service-0.0.1-SNAPSHOT.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "restaurant-service-0.0.1-SNAPSHOT.jar"]
CMD ["mvn", "spring-boot:run"]
