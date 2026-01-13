# 🚀 BeeFood Platform – Installation & Configuration Guide

This guide provides step-by-step instructions to configure, install, and run the BeeFood microservices platform for development.

---

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [System Requirements](#system-requirements)
- [Initial Setup](#initial-setup)
- [Infrastructure Setup](#infrastructure-setup)
- [Microservices Configuration](#microservices-configuration)
- [Running Services](#running-services)
- [Testing the Platform](#testing-the-platform)
- [Troubleshooting](#troubleshooting)
- [Useful Commands](#useful-commands)

---

## Prerequisites

Before starting, ensure you have the following installed:

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| **Java JDK** | 21+ | Runtime for Spring Boot applications |
| **Maven** | 3.8+ | Build tool |
| **Docker** | 20.10+ | Container runtime |
| **Docker Compose** | 2.0+ | Multi-container orchestration |
| **PostgreSQL** | 14+ | Relational database (or use Docker) |
| **MongoDB** | 5.0+ | NoSQL database (or use Docker) |
| **Redis** | 7.0+ | Cache layer (or use Docker) |
| **Apache Kafka** | 3.0+ | Event streaming (or use Docker) |

### Verify Installations

```bash
# Check Java version
java -version

# Check Maven version
mvn -version

# Check Docker version
docker --version
docker-compose --version

# Check Git
git --version
```

---

## System Requirements

### Hardware

- **RAM:** 8GB minimum, 16GB recommended
- **CPU:** 4 cores minimum, 8 cores recommended
- **Disk Space:** 10GB free space

### Network Ports

Ensure the following ports are available:

| Port | Service | Description |
|------|---------|-------------|
| 8080 | API Gateway | Single entry point |
| 8761 | Eureka Server | Service discovery |
| 8082 | Users Service | User management |
| 8083 | Products Service | Product catalog |
| 8084 | Restaurants Service | Restaurant management |
| 8085 | Orders Service | Order processing |
| 8086 | Delivery Service | Delivery logistics |
| 8087 | Notification Service | Notifications |
| 5432 | PostgreSQL | Relational database |
| 27017 | MongoDB | NoSQL database |
| 6379 | Redis | Cache layer |
| 9092 | Kafka | Event streaming |
| 2181 | Zookeeper | Kafka coordination |

---

## Initial Setup

### 1. Clone the Repository

```bash
git clone https://github.com/minhduc59/beefood.git
cd BeeFood
```

### 2. Project Structure Overview

```
BeeFood/
├── api-gateway/              # Spring Cloud Gateway (Port 8080)
├── discovery-service/        # Eureka Server (Port 8761)
├── user-service/            # Users + Auth (Port 8082)
├── product-service/         # Products (Port 8083)
├── restaurant-service/      # Restaurants (Port 8084)
├── order-service/          # Orders (Port 8085)
├── delivery-service/       # Delivery (Port 8086)
├── notification-service/   # Notifications (Port 8087)
├── README.md               # Project overview
└── copilot-instructions.md # Development guidelines
```

---

## Infrastructure Setup

You can run infrastructure components **locally** or using **Docker Compose**.

### Option A: Docker Compose (Recommended)

Create a `docker-compose-infrastructure.yml` file in the root directory:

```yaml
version: '3.9'

services:
  # -----------------------------
  # Eureka Server (Service Discovery)
  # -----------------------------
  eureka-server:
    build: ./discovery-service
    container_name: eureka-server
    ports:
      - "8761:8761"
    networks:
      - beefood-network
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8761/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5

  # -----------------------------
  # PostgreSQL (Users, Products, Orders)
  # -----------------------------
  postgres:
    image: postgres:16-alpine
    container_name: beefood-postgres
    environment:
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
      POSTGRES_MULTIPLE_DATABASES: user_service_db,product_service_db,order_service_db
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-databases.sh:/docker-entrypoint-initdb.d/init-databases.sh
    networks:
      - beefood-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin"]
      interval: 10s
      timeout: 5s
      retries: 5

  # -----------------------------
  # MongoDB (Restaurants, Delivery)
  # -----------------------------
  mongodb:
    image: mongo:7.0
    container_name: beefood-mongodb
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: admin123
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db
    networks:
      - beefood-network
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5

  # -----------------------------
  # Redis (Cache Layer)
  # -----------------------------
  redis:
    image: redis:7-alpine
    container_name: beefood-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - beefood-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # -----------------------------
  # Zookeeper (Kafka Dependency)
  # -----------------------------
  zookeeper:
    image: bitnami/zookeeper:3.9
    container_name: beefood-zookeeper
    ports:
      - "2181:2181"
    environment:
      ALLOW_ANONYMOUS_LOGIN: "yes"
    networks:
      - beefood-network

  # -----------------------------
  # Kafka (Event Streaming)
  # -----------------------------
  kafka:
    image: bitnami/kafka:3.6
    container_name: beefood-kafka
    ports:
      - "9092:9092"
    environment:
      KAFKA_CFG_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      ALLOW_PLAINTEXT_LISTENER: "yes"
    depends_on:
      - zookeeper
    networks:
      - beefood-network
    healthcheck:
      test: ["CMD-SHELL", "kafka-topics.sh --bootstrap-server localhost:9092 --list"]
      interval: 30s
      timeout: 10s
      retries: 5

volumes:
  postgres_data:
  mongo_data:
  redis_data:

networks:
  beefood-network:
    driver: bridge
```

**Create Database Initialization Script** (`scripts/init-databases.sh`):

```bash
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE user_service_db;
    CREATE DATABASE product_service_db;
    CREATE DATABASE order_service_db;
EOSQL
```

**Start Infrastructure:**

```bash
# Make script executable
chmod +x scripts/init-databases.sh

# Start all infrastructure services
docker-compose -f docker-compose-infrastructure.yml up -d

# Check status
docker-compose -f docker-compose-infrastructure.yml ps

# View logs
docker-compose -f docker-compose-infrastructure.yml logs -f
```

### Option B: Local Installation

#### PostgreSQL Setup

```bash
# Install PostgreSQL (macOS)
brew install postgresql@16

# Start PostgreSQL
brew services start postgresql@16

# Create databases
psql -U postgres
CREATE DATABASE user_service_db;
CREATE DATABASE product_service_db;
CREATE DATABASE order_service_db;
CREATE USER admin WITH PASSWORD 'admin123';
GRANT ALL PRIVILEGES ON DATABASE user_service_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE product_service_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE order_service_db TO admin;
\q
```

#### MongoDB Setup

```bash
# Install MongoDB (macOS)
brew tap mongodb/brew
brew install mongodb-community@7.0

# Start MongoDB
brew services start mongodb-community@7.0

# Create databases (auto-created on first connection)
mongosh
use restaurant_service_db
use delivery_service_db
exit
```

#### Redis Setup

```bash
# Install Redis (macOS)
brew install redis

# Start Redis
brew services start redis

# Test connection
redis-cli ping
```

#### Kafka Setup

```bash
# Install Kafka (macOS)
brew install kafka

# Start Zookeeper
zookeeper-server-start /opt/homebrew/etc/kafka/zookeeper.properties &

# Start Kafka
kafka-server-start /opt/homebrew/etc/kafka/server.properties &
```

---

## Microservices Configuration

### 1. Discovery Service (Eureka Server)

**File:** `discovery-service/src/main/resources/application.yml`

```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    hostname: localhost
```

**Build & Run:**

```bash
cd discovery-service
mvn clean install -DskipTests
mvn spring-boot:run
```

**Verify:** http://localhost:8761

---

### 2. API Gateway

**File:** `api-gateway/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: API-GATEWAY

  cloud:
    gateway:
      routes:
        - id: user-service-auth
          uri: lb://users
          predicates:
            - Path=/api/v1/auth/**,/api/v1/users/**
        
        - id: product-service
          uri: lb://products
          predicates:
            - Path=/api/v1/products/**
        
        - id: restaurant-service
          uri: lb://restaurants
          predicates:
            - Path=/api/v1/restaurants/**
        
        - id: order-service
          uri: lb://orders
          predicates:
            - Path=/api/v1/orders/**
        
        - id: delivery-service
          uri: lb://deliveries
          predicates:
            - Path=/api/v1/deliveries/**
        
        - id: notification-service
          uri: lb://notifications
          predicates:
            - Path=/api/v1/notifications/**

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**Build & Run:**

```bash
cd api-gateway
mvn clean install -DskipTests
mvn spring-boot:run
```

---

### 3. Users Service (+ Auth)

**File:** `user-service/src/main/resources/application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: users

  datasource:
    url: jdbc:postgresql://localhost:5432/user_service_db
    username: admin
    password: admin123
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

app:
  jwt:
    secret: your_jwt_secret_key_minimum_32_characters_long_replace_this
    expiration: 86400000      # 24 hours in milliseconds
    refresh-expiration: 604800000  # 7 days

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: false
    hostname: localhost
```

**Build & Run:**

```bash
cd user-service
mvn clean install -DskipTests
mvn spring-boot:run
```

---

### 4. Products Service

**File:** `product-service/src/main/resources/application.properties`

```properties
spring.application.name=products
server.port=8083

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/product_service_db
spring.datasource.username=admin
spring.datasource.password=admin123
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Redis Cache Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=600000

# Eureka Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.hostname=localhost
```

**Build & Run:**

```bash
cd product-service
mvn clean install -DskipTests
mvn spring-boot:run
```

---

### 5. Restaurants Service

**File:** `restaurant-service/src/main/resources/application.properties`

```properties
spring.application.name=restaurants
server.port=8084

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://admin:admin123@localhost:27017/restaurant_service_db?authSource=admin
spring.data.mongodb.database=restaurant_service_db

# Eureka Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.hostname=localhost
```

**Build & Run:**

```bash
cd restaurant-service
mvn clean install -DskipTests
mvn spring-boot:run
```

---

### 6. Orders Service

**File:** `order-service/src/main/resources/application.properties`

```properties
spring.application.name=orders
server.port=8085

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/order_service_db
spring.datasource.username=admin
spring.datasource.password=admin123
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Kafka Producer Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3

# Eureka Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.hostname=localhost
```

**Build & Run:**

```bash
cd order-service
mvn clean install -DskipTests
mvn spring-boot:run
```

---

### 7. Delivery Service

**File:** `delivery-service/src/main/resources/application.properties`

```properties
spring.application.name=deliveries
server.port=8086

# MongoDB Configuration
spring.data.mongodb.uri=mongodb://admin:admin123@localhost:27017/delivery_service_db?authSource=admin
spring.data.mongodb.database=delivery_service_db

# Kafka Consumer Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=delivery-service-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false

# Eureka Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.hostname=localhost
```

**Build & Run:**

```bash
cd delivery-service
mvn clean install -DskipTests
mvn spring-boot:run
```

---

### 8. Notification Service

**File:** `notification-service/src/main/resources/application.properties`

```properties
spring.application.name=notifications
server.port=8087

# Kafka Consumer Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.auto-offset-reset=earliest

# Eureka Configuration
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.instance.hostname=localhost
```

**Build & Run:**

```bash
cd notification-service
mvn clean install -DskipTests
mvn spring-boot:run
```

---

## Running Services

### Startup Order (Important!)

Services must be started in this order:

1. **Infrastructure Layer** (Postgres, MongoDB, Redis, Kafka, Zookeeper)
2. **Discovery Service** (Eureka)
3. **API Gateway**
4. **Microservices** (Users, Products, Restaurants, Orders, Delivery, Notification)

### Option 1: Manual Startup (Development)

**Terminal 1 - Eureka:**
```bash
cd discovery-service
mvn spring-boot:run
```

**Terminal 2 - Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
```

**Terminal 3 - Users:**
```bash
cd user-service
mvn spring-boot:run
```

**Terminal 4 - Products:**
```bash
cd product-service
mvn spring-boot:run
```

**Terminal 5 - Restaurants:**
```bash
cd restaurant-service
mvn spring-boot:run
```

**Terminal 6 - Orders:**
```bash
cd order-service
mvn spring-boot:run
```

**Terminal 7 - Delivery:**
```bash
cd delivery-service
mvn spring-boot:run
```

**Terminal 8 - Notifications:**
```bash
cd notification-service
mvn spring-boot:run
```

### Option 2: Build All Services (Automated)

Create a script `build-all.sh`:

```bash
#!/bin/bash

echo "Building all BeeFood microservices..."

services=("discovery-service" "api-gateway" "user-service" "product-service" "restaurant-service" "order-service" "delivery-service" "notification-service")

for service in "${services[@]}"; do
    echo "Building $service..."
    cd $service
    mvn clean install -DskipTests
    cd ..
done

echo "All services built successfully!"
```

```bash
chmod +x scripts/build-all.sh
./scripts/build-all.sh
```

### Option 3: Docker Compose (Full Platform)

Create `docker-compose.yml` in root:

```yaml
version: '3.9'

services:
  eureka-server:
    build: ./discovery-service
    ports:
      - "8761:8761"
    networks:
      - beefood-network

  api-gateway:
    build: ./api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server
    environment:
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka/
    networks:
      - beefood-network

  user-service:
    build: ./user-service
    ports:
      - "8082:8082"
    depends_on:
      - postgres
      - eureka-server
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/user_service_db
      EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE: http://eureka-server:8761/eureka/
    networks:
      - beefood-network

  # Add other services similarly...

networks:
  beefood-network:
    driver: bridge
```

```bash
docker-compose up -d
```

---

## Testing the Platform

### 1. Verify Service Registration

Visit Eureka Dashboard: http://localhost:8761

You should see all services registered:
- API-GATEWAY
- users
- products
- restaurants
- orders
- deliveries
- notifications

### 2. Test User Registration (via Gateway)

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@beefood.com",
    "password": "Test123!",
    "role": "CUSTOMER"
  }'
```

### 3. Test User Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@beefood.com",
    "password": "Test123!"
  }'
```

Response should include JWT token:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1...",
  "refreshToken": "eyJhbGciOiJIUzI1...",
  "tokenType": "Bearer"
}
```

### 4. Test Authenticated Request

```bash
export TOKEN="your_access_token_here"

curl -X GET http://localhost:8080/api/v1/users/profile \
  -H "Authorization: Bearer $TOKEN"
```

### 5. Test Kafka Event Flow

Create an order (will publish `ORDER_CREATED` event):

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": "REST001",
    "items": [
      {"productId": "PROD001", "quantity": 2}
    ]
  }'
```

Check Delivery Service logs to verify event consumption.

---

## Troubleshooting

### Problem: Service Not Registering with Eureka

**Symptoms:**
- Service starts but doesn't appear in Eureka dashboard
- `Connection refused` errors in logs

**Solutions:**

1. **Verify Eureka is running:**
   ```bash
   curl http://localhost:8761/actuator/health
   ```

2. **Check application properties:**
   ```properties
   eureka.client.register-with-eureka=true
   eureka.client.fetch-registry=true
   eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
   ```

3. **Check network connectivity (Docker):**
   ```bash
   docker network inspect beefood-network
   ```

---

### Problem: Database Connection Failed

**Symptoms:**
- `Connection refused` or `FATAL: password authentication failed`

**Solutions:**

1. **Verify database is running:**
   ```bash
   # PostgreSQL
   psql -U admin -d user_service_db -h localhost
   
   # MongoDB
   mongosh mongodb://admin:admin123@localhost:27017
   ```

2. **Check credentials in application.properties:**
   ```properties
   spring.datasource.username=admin
   spring.datasource.password=admin123
   ```

3. **Create missing databases:**
   ```sql
   CREATE DATABASE user_service_db;
   CREATE DATABASE product_service_db;
   CREATE DATABASE order_service_db;
   ```

---

### Problem: Port Already in Use

**Symptoms:**
- `Address already in use` or `Port 8080 is already allocated`

**Solutions:**

1. **Find process using port:**
   ```bash
   lsof -i :8080
   ```

2. **Kill process:**
   ```bash
   kill -9 <PID>
   ```

3. **Change port in application.properties:**
   ```properties
   server.port=8081
   ```

---

### Problem: Kafka Consumer Not Receiving Events

**Symptoms:**
- Events published but not consumed
- No logs in consumer service

**Solutions:**

1. **Verify Kafka is running:**
   ```bash
   kafka-topics.sh --bootstrap-server localhost:9092 --list
   ```

2. **Check consumer group:**
   ```bash
   kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group delivery-service-group
   ```

3. **Verify topic exists:**
   ```bash
   kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic orders.created
   ```

4. **Check consumer configuration:**
   ```properties
   spring.kafka.consumer.group-id=delivery-service-group
   spring.kafka.consumer.auto-offset-reset=earliest
   ```

---

### Problem: Redis Cache Not Working

**Symptoms:**
- No performance improvement
- Cache miss on every request

**Solutions:**

1. **Verify Redis is running:**
   ```bash
   redis-cli ping
   ```

2. **Enable caching in Spring Boot:**
   ```java
   @EnableCaching
   public class ProductServiceApplication { }
   ```

3. **Check cache configuration:**
   ```properties
   spring.cache.type=redis
   spring.data.redis.host=localhost
   spring.data.redis.port=6379
   ```

---

### Problem: JWT Token Validation Failed

**Symptoms:**
- `401 Unauthorized` on authenticated endpoints
- `Invalid JWT signature`

**Solutions:**

1. **Ensure same secret across services:**
   ```properties
   app.jwt.secret=your_jwt_secret_key_minimum_32_characters_long_replace_this
   ```

2. **Check token expiration:**
   ```properties
   app.jwt.expiration=86400000  # 24 hours
   ```

3. **Verify token format:**
   ```bash
   Authorization: Bearer eyJhbGciOiJIUzI1...
   ```

---

### Problem: Maven Build Fails

**Symptoms:**
- `BUILD FAILURE` or dependency resolution errors

**Solutions:**

1. **Clean Maven cache:**
   ```bash
   mvn clean install -U
   ```

2. **Delete local repository:**
   ```bash
   rm -rf ~/.m2/repository
   mvn clean install
   ```

3. **Skip tests during build:**
   ```bash
   mvn clean install -DskipTests
   ```

---

### Problem: Docker Container Exits Immediately

**Symptoms:**
- Container starts then stops
- `Exited (1)` status

**Solutions:**

1. **Check logs:**
   ```bash
   docker logs <container_name>
   ```

2. **Verify Dockerfile:**
   ```dockerfile
   EXPOSE 8082
   ENTRYPOINT ["java", "-jar", "app.jar"]
   ```

3. **Check health checks:**
   ```bash
   docker inspect <container_name> | grep Health
   ```

---

## Useful Commands

### Maven Commands

```bash
# Clean and build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run service
mvn spring-boot:run

# Package as JAR
mvn package

# Run tests only
mvn test
```

### Docker Commands

```bash
# Build image
docker build -t user-service:latest ./user-service

# Run container
docker run -d -p 8082:8082 --name user-service user-service:latest

# View logs
docker logs -f user-service

# Stop container
docker stop user-service

# Remove container
docker rm user-service

# Prune all
docker system prune -a
```

### Kafka Commands

```bash
# List topics
kafka-topics.sh --bootstrap-server localhost:9092 --list

# Create topic
kafka-topics.sh --bootstrap-server localhost:9092 --create --topic orders.created --partitions 3 --replication-factor 1

# Describe topic
kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic orders.created

# Consume messages
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders.created --from-beginning

# List consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Describe consumer group
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group delivery-service-group
```

### PostgreSQL Commands

```bash
# Connect to database
psql -U admin -d user_service_db

# List databases
\l

# Connect to database
\c user_service_db

# List tables
\dt

# Describe table
\d users

# Quit
\q
```

### MongoDB Commands

```bash
# Connect to MongoDB
mongosh mongodb://admin:admin123@localhost:27017

# Show databases
show dbs

# Use database
use restaurant_service_db

# Show collections
show collections

# Query documents
db.restaurants.find().pretty()

# Exit
exit
```

### Redis Commands

```bash
# Connect to Redis
redis-cli

# Check connection
PING

# List all keys
KEYS *

# Get value
GET products:123

# Delete key
DEL products:123

# Flush all
FLUSHALL

# Exit
exit
```

---

## Next Steps

After successfully running the platform:

1. **Explore API Documentation:** Add Swagger/OpenAPI to each service
2. **Set Up Monitoring:** Integrate with Prometheus + Grafana
3. **Add Distributed Tracing:** Integrate Zipkin or Jaeger
4. **Configure Logging:** Set up ELK Stack (Elasticsearch, Logstash, Kibana)
5. **Write Tests:** Add unit, integration, and end-to-end tests
6. **Deploy to Cloud:** Use Kubernetes for production deployment

---
# BeeFood Local Development - Quick Reference

## 🚀 Quick Start

```bash
# 1. Start infrastructure
./scripts/run-services-locally.sh

# 2. Open 8 terminal tabs and run these commands:

# Tab 1: Discovery Service
cd discovery-service && mvn spring-boot:run

# Tab 2: API Gateway (wait for Tab 1 to finish starting)
cd api-gateway && mvn spring-boot:run

# Tab 3: User Service
cd user-service && mvn spring-boot:run

# Tab 4: Product Service
cd product-service && mvn spring-boot:run

# Tab 5: Restaurant Service
cd restaurant-service && mvn spring-boot:run

# Tab 6: Order Service
cd order-service && mvn spring-boot:run

# Tab 7: Delivery Service
cd delivery-service && mvn spring-boot:run

# Tab 8: Notification Service
cd notification-service && mvn spring-boot:run 

## Additional Resources

- **Spring Boot Documentation:** https://spring.io/projects/spring-boot
- **Spring Cloud Gateway:** https://spring.io/projects/spring-cloud-gateway
- **Apache Kafka:** https://kafka.apache.org/documentation/
- **Docker Documentation:** https://docs.docker.com/
- **PostgreSQL Documentation:** https://www.postgresql.org/docs/
- **MongoDB Documentation:** https://www.mongodb.com/docs/

---

## Support

For issues or questions:
- Check **Troubleshooting** section above
- Review `copilot-instructions.md` for architecture guidelines
- Open an issue on GitHub repository

---

**Version:** 1.0  
**Last Updated:** December 26, 2025  
**Maintained by:** BeeFood Development Team
