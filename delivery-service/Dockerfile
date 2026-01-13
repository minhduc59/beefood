# Dockerfile for Delivery Service
FROM maven:latest
VOLUME /tmp
ARG PROJECT_VERSION=0.0.1
RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE application
COPY ./ .
ADD target/delivery-service-0.0.1-SNAPSHOT.jar delivery-service-0.0.1-SNAPSHOT.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "delivery-service-0.0.1-SNAPSHOT.jar"]
CMD ["mvn", "spring-boot:run"]
