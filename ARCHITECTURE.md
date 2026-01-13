# 🏗️ BeeFood Platform - Architecture & Docker Guide

## 📋 Table of Contents

- [System Architecture Overview](#system-architecture-overview)
- [Microservices Architecture](#microservices-architecture)
- [Docker Architecture](#docker-architecture)
- [Service Roles & Responsibilities](#service-roles--responsibilities)
- [Communication Patterns](#communication-patterns)
- [Docker Usage Guide](#docker-usage-guide)
- [Development Workflow](#development-workflow)
- [Production Considerations](#production-considerations)

---

## 🎯 System Architecture Overview

BeeFood is a **cloud-native food ordering and delivery platform** built using **microservices architecture** and **event-driven design patterns**. The system is designed for high scalability, fault tolerance, and independent service deployment.

### Architecture Principles

1. **Database-per-Service Pattern**: Each microservice owns its own database
2. **API Gateway Pattern**: Single entry point for all client requests
3. **Service Discovery**: Dynamic service registration and lookup
4. **Event-Driven Architecture**: Asynchronous communication via Kafka
5. **Containerization**: All services run in Docker containers

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                              │
│              (Web App, Mobile App, Third-party APIs)            │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API GATEWAY (8080)                          │
│              • JWT Authentication & Authorization                │
│              • Request Routing & Load Balancing                  │
│              • Rate Limiting & Circuit Breaking                  │
└──┬───────┬───────┬───────┬───────┬───────┬───────┬──────────────┘
   │       │       │       │       │       │       │
   ▼       ▼       ▼       ▼       ▼       ▼       ▼
┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│Users │ │Prod  │ │Rest  │ │Order │ │Deliv │ │Notif │ │Eureka│
│8082  │ │8083  │ │8084  │ │8085  │ │8086  │ │8087  │ │8761  │
└──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘ └──────┘
   │        │        │        │        │        │
   ▼        ▼        ▼        ▼        ▼        ▼
┌──────────────────────────────────────────────────────────────┐
│              DATA & MESSAGING LAYER                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │PostgreSQL│  │ MongoDB  │  │  Redis   │  │  Kafka   │    │
│  │  5432    │  │  27017   │  │  6379    │  │  9092    │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└──────────────────────────────────────────────────────────────┘
```

---

## 🏢 Microservices Architecture

### 1. Infrastructure Layer

#### **PostgreSQL (Port 5432)**
- **Role**: Relational database for structured data
- **Services Using It**: Users, Products, Orders
- **Pattern**: Database-per-Service
- **Databases Created**:
  - `user_service_db` - User profiles and auth credentials
  - `product_service_db` - Product catalog and inventory
  - `order_service_db` - Orders and payment records

**Why PostgreSQL?**
- ACID compliance for transactional data
- Strong consistency guarantees
- Complex queries and joins within service boundaries
- Mature ecosystem and tooling

#### **MongoDB (Port 27017)**
- **Role**: NoSQL database for flexible document storage
- **Services Using It**: Restaurants, Delivery
- **Pattern**: Document-oriented storage
- **Databases Created**:
  - `restaurant_service_db` - Restaurant profiles and dynamic menus
  - `delivery_service_db` - Delivery records and driver locations

**Why MongoDB?**
- Flexible schema for evolving menu structures
- Geospatial queries for delivery tracking
- High write throughput for location updates
- Document model matches domain objects

#### **Redis (Port 6379)**
- **Role**: In-memory cache layer
- **Services Using It**: Products Service
- **Pattern**: Cache-Aside (Lazy Loading)
- **Use Cases**:
  - Cache popular products (TTL: 10 minutes)
  - Cache restaurant menus (TTL: 5 minutes)
  - Cache search results (TTL: 5 minutes)

**Why Redis?**
- Sub-millisecond latency
- Reduces database load by 60-80%
- Simple key-value operations
- Built-in TTL support

#### **Apache Kafka (Port 9092)**
- **Role**: Event streaming platform
- **Pattern**: Publish-Subscribe (Pub-Sub)
- **Topics**:
  - `orders.created` - New order events
  - `orders.confirmed` - Order confirmation events
  - `orders.cancelled` - Order cancellation events
  - `deliveries.assigned` - Delivery assignment events
  - `payments.completed` - Payment success events

**Why Kafka?**
- Decouples services (loose coupling)
- Asynchronous processing
- Event replay capability
- High throughput (100k+ events/sec)
- Fault tolerance and durability

#### **Zookeeper (Port 2181)**
- **Role**: Kafka cluster coordination
- **Responsibilities**:
  - Kafka broker metadata management
  - Leader election for partitions
  - Configuration management

---

### 2. Service Discovery & Gateway Layer

#### **Eureka Server (Port 8761)**
- **Role**: Service registry and discovery
- **Pattern**: Client-Side Service Discovery
- **Responsibilities**:
  - Service registration (all microservices register on startup)
  - Health monitoring (heartbeat mechanism)
  - Service lookup (Gateway queries for service instances)
  - Load balancing information

**How It Works**:
1. Microservices register with Eureka on startup
2. Services send heartbeats every 30 seconds
3. Gateway queries Eureka to find service instances
4. Gateway load-balances requests across instances

#### **API Gateway (Port 8080)**
- **Role**: Single entry point for all client requests
- **Pattern**: Gateway Aggregation + API Gateway
- **Responsibilities**:
  - **Routing**: Routes requests to appropriate microservices
  - **Authentication**: Validates JWT tokens
  - **Authorization**: Enforces role-based access control
  - **Load Balancing**: Distributes requests across service instances
  - **Rate Limiting**: Prevents API abuse
  - **Request/Response Transformation**: Adapts protocols

**Request Flow**:
```
Client → Gateway → JWT Validation → Route to Service → Response
```

**Routing Example**:
- `POST /api/v1/auth/login` → Users Service
- `GET /api/v1/products` → Products Service
- `POST /api/v1/orders` → Orders Service

---

### 3. Business Services Layer

#### **Users Service (Port 8082)**
- **Database**: PostgreSQL (`user_service_db`)
- **Purpose**: User management and authentication
- **Responsibilities**:
  - User registration (creates auth credentials)
  - User login (issues JWT tokens)
  - JWT token validation and refresh
  - User profile CRUD operations
  - Role management (CUSTOMER, RESTAURANT_OWNER, DRIVER, ADMIN)
  - Password reset flows

**API Endpoints**:
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login and get JWT
- `POST /api/v1/auth/refresh` - Refresh access token
- `GET /api/v1/users/profile` - Get user profile
- `PUT /api/v1/users/profile` - Update profile

**Communication Pattern**: REST API (synchronous)

#### **Products Service (Port 8083)**
- **Database**: PostgreSQL (`product_service_db`)
- **Cache**: Redis
- **Purpose**: Product catalog management
- **Responsibilities**:
  - Product CRUD operations
  - Inventory management
  - Product search and filtering
  - Category management
  - Cache popular products
  - Consume `ORDER_CREATED` events to update inventory

**API Endpoints**:
- `GET /api/v1/products` - List all products (cached)
- `GET /api/v1/products/{id}` - Get product details (cached)
- `POST /api/v1/products` - Create product (admin only)
- `PUT /api/v1/products/{id}` - Update product (invalidate cache)
- `DELETE /api/v1/products/{id}` - Delete product

**Communication Pattern**: 
- REST API (read operations)
- Kafka Consumer (inventory updates)

**Caching Strategy**:
```java
@Cacheable(value = "products", key = "#id")
public Product getProductById(Long id) { ... }

@CacheEvict(value = "products", key = "#id")
public void updateProduct(Long id, Product product) { ... }
```

#### **Restaurants Service (Port 8084)**
- **Database**: MongoDB (`restaurant_service_db`)
- **Purpose**: Restaurant profile and menu management
- **Responsibilities**:
  - Restaurant CRUD operations
  - Menu management (flexible schema for variations)
  - Operating hours and status (open/closed)
  - Restaurant search by location/cuisine
  - Rating and review aggregation

**API Endpoints**:
- `GET /api/v1/restaurants` - List restaurants
- `GET /api/v1/restaurants/{id}` - Get restaurant details
- `GET /api/v1/restaurants/{id}/menu` - Get restaurant menu
- `POST /api/v1/restaurants` - Create restaurant (owner/admin)
- `PUT /api/v1/restaurants/{id}` - Update restaurant

**Communication Pattern**: REST API (synchronous)

**MongoDB Document Example**:
```json
{
  "_id": "REST001",
  "name": "Pizza Palace",
  "location": {
    "type": "Point",
    "coordinates": [-73.935242, 40.730610]
  },
  "menu": [
    {
      "category": "Pizza",
      "items": [
        {
          "name": "Margherita",
          "price": 12.99,
          "variations": ["Small", "Medium", "Large"]
        }
      ]
    }
  ],
  "operatingHours": {
    "monday": { "open": "10:00", "close": "22:00" }
  }
}
```

#### **Orders Service (Port 8085) - CORE SERVICE**
- **Database**: PostgreSQL (`order_service_db`)
- **Kafka**: Producer
- **Purpose**: Order lifecycle and payment processing
- **Responsibilities**:
  - Create and validate orders
  - Process payments (integrate with payment gateway)
  - Manage order state machine
  - Publish Kafka events (`ORDER_CREATED`, `ORDER_CONFIRMED`, `ORDER_CANCELLED`)
  - Query order history

**API Endpoints**:
- `POST /api/v1/orders` - Create new order
- `GET /api/v1/orders/{id}` - Get order details
- `GET /api/v1/orders/history` - Get user order history
- `PUT /api/v1/orders/{id}/status` - Update order status
- `POST /api/v1/orders/{id}/cancel` - Cancel order

**Communication Pattern**:
- REST API (order creation)
- Kafka Producer (order events)

**Order State Machine**:
```
PENDING (initial state)
   ↓
CONFIRMED (payment successful)
   ↓
PREPARING (restaurant preparing food)
   ↓
READY (ready for pickup)
   ↓
PICKED_UP (driver collected order)
   ↓
DELIVERED (final state)

CANCELLED (can only cancel before PREPARING)
```

**Kafka Event Example**:
```json
{
  "eventId": "evt-12345",
  "eventType": "ORDER_CREATED",
  "timestamp": "2025-12-29T10:30:00Z",
  "version": "1.0",
  "payload": {
    "orderId": "ORD-12345",
    "userId": "USR-789",
    "restaurantId": "REST-001",
    "totalAmount": 45.99,
    "items": [...]
  }
}
```

#### **Delivery Service (Port 8086)**
- **Database**: MongoDB (`delivery_service_db`)
- **Kafka**: Consumer
- **Purpose**: Delivery logistics and driver management
- **Responsibilities**:
  - Consume `ORDER_CONFIRMED` events
  - Assign delivery drivers (algorithm-based)
  - Track delivery status in real-time
  - Update delivery location (GPS tracking)
  - Calculate delivery ETA

**API Endpoints**:
- `GET /api/v1/deliveries/{orderId}` - Track delivery
- `PUT /api/v1/deliveries/{id}/location` - Update driver location
- `PUT /api/v1/deliveries/{id}/status` - Update delivery status

**Communication Pattern**:
- Kafka Consumer (order events)
- REST API (delivery tracking)

**Delivery Assignment Logic**:
```java
@KafkaListener(topics = "orders.confirmed")
public void handleOrderConfirmed(OrderConfirmedEvent event) {
    // 1. Find available drivers near restaurant
    List<Driver> drivers = driverRepository.findNearby(event.getRestaurantLocation());
    
    // 2. Assign driver with best score (distance + rating)
    Driver assignedDriver = assignmentAlgorithm.selectBestDriver(drivers);
    
    // 3. Create delivery record
    Delivery delivery = new Delivery(event.getOrderId(), assignedDriver.getId());
    deliveryRepository.save(delivery);
    
    // 4. Publish DELIVERY_ASSIGNED event
    kafkaTemplate.send("deliveries.assigned", new DeliveryAssignedEvent(delivery));
}
```

#### **Notification Service (Port 8087)**
- **Kafka**: Consumer (all topics)
- **Purpose**: Multi-channel notifications
- **Responsibilities**:
  - Consume events from all services
  - Send email notifications (order confirmations, receipts)
  - Send SMS notifications (delivery updates)
  - Send push notifications (real-time alerts)
  - Log notification history

**Communication Pattern**: Kafka Consumer (fan-out pattern)

**Notification Rules**:
- `ORDER_CREATED` → Email receipt to customer
- `ORDER_CONFIRMED` → SMS to customer + restaurant
- `DELIVERY_ASSIGNED` → Push notification to customer
- `ORDER_DELIVERED` → Email + SMS confirmation

---

## 🐳 Docker Architecture

### Why Docker?

Docker provides:
1. **Consistency**: Same environment across dev, test, prod
2. **Isolation**: Services run in isolated containers
3. **Portability**: Run anywhere (local, cloud, on-premises)
4. **Scalability**: Easily scale services horizontally
5. **Resource Efficiency**: Lightweight compared to VMs

### Docker Components in BeeFood

#### 1. **Dockerfile** (Service-Specific)

Each microservice has a Dockerfile that defines how to build its container image.

**Example: Users Service Dockerfile**
```dockerfile
FROM maven:latest
VOLUME /tmp
ARG PROJECT_VERSION=0.0.1
RUN mkdir -p /home/app
WORKDIR /home/app
ENV SPRING_PROFILES_ACTIVE application
COPY ./ .
ADD target/user-service-0.0.1-SNAPSHOT.jar user-service-0.0.1-SNAPSHOT.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "user-service-0.0.1-SNAPSHOT.jar"]
CMD ["mvn", "spring-boot:run"]
```

**What Each Line Does**:
- `FROM maven:latest` - Base image with Java + Maven
- `VOLUME /tmp` - Mount point for temporary files
- `WORKDIR /home/app` - Set working directory
- `COPY ./ .` - Copy service code into container
- `ADD target/*.jar` - Add compiled JAR file
- `EXPOSE 8082` - Document port (doesn't publish)
- `ENTRYPOINT` - Default command to run service

#### 2. **docker-compose.yml** (Orchestration)

Defines all services, their dependencies, networks, and volumes.

**Key Sections**:

**Service Definition**:
```yaml
user-service:
  build: ./user-service          # Dockerfile location
  container_name: user-service   # Container name
  ports:
    - "8082:8082"                # Port mapping (host:container)
  depends_on:                    # Start order
    postgres:
      condition: service_healthy
  environment:                   # Environment variables
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/user_service_db
  networks:
    - beefood-network            # Network to join
  healthcheck:                   # Health monitoring
    test: ["CMD-SHELL", "curl -f http://localhost:8082/actuator/health"]
  restart: unless-stopped        # Restart policy
```

**Networks**:
```yaml
networks:
  beefood-network:
    driver: bridge
```
- All services share the `beefood-network`
- Services reference each other by name (e.g., `postgres:5432`)

**Volumes**:
```yaml
volumes:
  postgres_data:
    driver: local
  mongo_data:
    driver: local
  redis_data:
    driver: local
```
- Persistent storage for databases
- Data survives container restarts

#### 3. **Service Dependencies**

Docker Compose ensures services start in correct order:

```
Infrastructure (Postgres, MongoDB, Redis, Kafka, Zookeeper)
   ↓
Eureka Server (Service Discovery)
   ↓
API Gateway
   ↓
Microservices (Users, Products, Restaurants, Orders, Delivery, Notification)
```

**Health Checks** ensure dependencies are ready before dependent services start.

---

## 📡 Communication Patterns

### 1. Synchronous Communication (REST API)

**Use Cases**:
- User login (immediate response needed)
- Product search (low latency)
- Order creation (user waiting for confirmation)

**Flow**:
```
Client → Gateway → Service → Database → Response
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Gateway routes to Products Service
# Products Service checks Redis cache
# On cache miss, queries PostgreSQL
# Returns response to client
```

### 2. Asynchronous Communication (Kafka)

**Use Cases**:
- Order confirmation (eventual consistency acceptable)
- Delivery assignment (background process)
- Notifications (fire-and-forget)

**Flow**:
```
Orders Service → Kafka → Delivery Service
                      → Notification Service
```

**Example**:
```java
// Orders Service (Producer)
OrderCreatedEvent event = new OrderCreatedEvent(order);
kafkaTemplate.send("orders.created", event);

// Delivery Service (Consumer)
@KafkaListener(topics = "orders.created")
public void handleOrderCreated(OrderCreatedEvent event) {
    // Process event (idempotent)
}
```

### 3. Service Discovery

Services don't hardcode URLs; they query Eureka:

```java
// Gateway uses LoadBalanced RestTemplate
@LoadBalanced
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}

// Call Users Service by name (Eureka resolves IP)
String url = "http://users/api/v1/users/profile";
ResponseEntity<UserProfile> response = restTemplate.getForEntity(url, UserProfile.class);
```

---

## 🚀 Docker Usage Guide

### Prerequisites

```bash
# Ensure Docker and Docker Compose are installed
docker --version
docker-compose --version

# Build all services first
./scripts/build-all.sh
```

### Starting the Platform

#### Option 1: Start Everything
```bash
# Start all services (infrastructure + microservices)
docker-compose up -d

# View logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f user-service
```

#### Option 2: Start Infrastructure Only
```bash
# Start only databases, Kafka, Redis, Eureka
docker-compose up -d postgres mongodb redis zookeeper kafka eureka-server

# Run microservices locally (for development)
cd user-service && mvn spring-boot:run
```

#### Option 3: Start Specific Services
```bash
# Start Users and Orders services only
docker-compose up -d postgres eureka-server api-gateway user-service order-service
```

### Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes all data)
docker-compose down -v

# Stop specific service
docker-compose stop user-service
```

### Scaling Services

```bash
# Run 3 instances of Orders Service
docker-compose up -d --scale order-service=3

# Gateway + Eureka will load-balance requests
```

### Viewing Service Status

```bash
# List running containers
docker-compose ps

# Check service health
docker-compose ps
curl http://localhost:8761  # Eureka dashboard
```

### Debugging

```bash
# View logs
docker-compose logs -f user-service

# Execute command inside container
docker-compose exec user-service sh

# View container resource usage
docker stats
```

### Rebuilding Services

```bash
# Rebuild specific service
docker-compose build user-service

# Rebuild and restart
docker-compose up -d --build user-service

# Rebuild all services
docker-compose build
docker-compose up -d
```

---

## 💻 Development Workflow

### Local Development (Hybrid Approach)

**Recommended for active development**:

1. **Run infrastructure in Docker**:
```bash
docker-compose up -d postgres mongodb redis kafka zookeeper eureka-server
```

2. **Run services locally**:
```bash
# Terminal 1
cd user-service
mvn spring-boot:run

# Terminal 2
cd product-service
mvn spring-boot:run

# Terminal 3
cd order-service
mvn spring-boot:run
```

**Benefits**:
- Faster rebuild cycles (no Docker rebuild)
- Easier debugging (attach debugger directly)
- Hot reload (Spring DevTools)

### Full Docker Development

**Best for testing full system**:

```bash
# Build all services
./scripts/build-all.sh

# Start everything
docker-compose up -d

# Make code changes
# Rebuild and restart specific service
docker-compose up -d --build user-service
```

### Testing Workflow

```bash
# Start infrastructure
docker-compose up -d postgres mongodb redis

# Run tests (services connect to Docker infrastructure)
cd user-service
mvn test

# Run integration tests
mvn verify
```

---

## 🌐 Production Considerations

### 1. Environment-Specific Configurations

Create Spring profiles for different environments:

**application-docker.yml** (for Docker):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/user_service_db  # Service name

eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/  # Service name
```

**application.yml** (for local):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/user_service_db  # Localhost

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # Localhost
```

### 2. Security Hardening

**Don't use in production**:
- Default passwords (`admin123`)
- Exposed ports for internal services
- `restart: unless-stopped` without monitoring

**Do in production**:
- Use secrets management (HashiCorp Vault, AWS Secrets Manager)
- Expose only Gateway port (8080) externally
- Implement centralized logging (ELK Stack)
- Add monitoring (Prometheus + Grafana)
- Use HTTPS with SSL certificates

### 3. Kubernetes Migration

For production, migrate from Docker Compose to Kubernetes:

**Why Kubernetes?**
- Auto-scaling based on metrics
- Self-healing (restarts failed containers)
- Rolling updates (zero-downtime deployments)
- Service mesh (Istio) for advanced traffic management

**Migration Path**:
```
Docker Compose → Kompose (convert to K8s manifests) → Kubernetes
```

### 4. Database Backups

```bash
# Backup PostgreSQL
docker-compose exec postgres pg_dump -U admin user_service_db > backup.sql

# Backup MongoDB
docker-compose exec mongodb mongodump --uri="mongodb://admin:admin123@localhost:27017" --out=/backup

# Restore PostgreSQL
docker-compose exec -T postgres psql -U admin user_service_db < backup.sql
```

### 5. Monitoring & Observability

**Add to docker-compose.yml**:
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **Zipkin/Jaeger**: Distributed tracing
- **ELK Stack**: Centralized logging

---

## 📊 Resource Requirements

### Development Environment

| Service | CPU | RAM | Disk |
|---------|-----|-----|------|
| PostgreSQL | 0.5 | 512MB | 2GB |
| MongoDB | 0.5 | 512MB | 2GB |
| Redis | 0.25 | 256MB | 512MB |
| Kafka + Zookeeper | 1.0 | 1GB | 2GB |
| Eureka | 0.5 | 512MB | 100MB |
| Gateway | 0.5 | 512MB | 100MB |
| Each Microservice | 0.5 | 512MB | 100MB |
| **TOTAL** | **4-6 cores** | **8-12GB** | **10GB** |

### Production Environment (Recommended)

| Service | Instances | CPU/Instance | RAM/Instance |
|---------|-----------|--------------|--------------|
| PostgreSQL | 1 (HA: 3) | 2 | 4GB |
| MongoDB | 1 (HA: 3) | 2 | 4GB |
| Redis | 1 (HA: 3) | 1 | 2GB |
| Kafka | 3 | 2 | 4GB |
| Eureka | 2 | 1 | 1GB |
| Gateway | 3 | 2 | 2GB |
| Microservices | 3 each | 1 | 2GB |

---

## 🔍 Troubleshooting Docker Issues

### Container Won't Start

```bash
# Check logs
docker-compose logs <service-name>

# Common issues:
# 1. Port already in use
lsof -i :8080
kill -9 <PID>

# 2. Database not ready
# Solution: Add healthcheck dependencies

# 3. JAR file missing
# Solution: Run ./scripts/build-all.sh first
```

### Network Issues

```bash
# Inspect network
docker network inspect beefood_beefood-network

# Ping between containers
docker-compose exec user-service ping postgres

# DNS resolution test
docker-compose exec user-service nslookup postgres
```

### Database Connection Failed

```bash
# Check if database is healthy
docker-compose ps

# Check database logs
docker-compose logs postgres

# Verify connection from container
docker-compose exec user-service sh
curl postgres:5432  # Should refuse connection (PostgreSQL binary protocol)
```

### Volume Issues

```bash
# Remove volumes and recreate
docker-compose down -v
docker-compose up -d

# Inspect volume
docker volume inspect beefood_postgres_data
```

---

## 📚 Additional Resources

- **Docker Documentation**: https://docs.docker.com/
- **Docker Compose Reference**: https://docs.docker.com/compose/
- **Spring Cloud Gateway**: https://spring.io/projects/spring-cloud-gateway
- **Netflix Eureka**: https://github.com/Netflix/eureka
- **Apache Kafka**: https://kafka.apache.org/documentation/

---

**Version**: 1.0  
**Last Updated**: December 29, 2025  
**Maintained by**: BeeFood Development Team
