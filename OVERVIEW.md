# 📊 BeeFood Platform - Complete Overview

## 🎯 What is BeeFood?

BeeFood is a production-grade microservices platform for food ordering and delivery, similar to ShopeeFood/UberEats. It demonstrates real-world patterns for building scalable, fault-tolerant distributed systems.

---

## 🏗️ Architecture at a Glance

```
┌─────────────────────────────────────────────────────────────┐
│                   CLIENT APPLICATIONS                        │
│         (Web Browser, Mobile App, Partner APIs)             │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              🚪 API GATEWAY (Port 8080)                      │
│  ✓ JWT Authentication    ✓ Rate Limiting                    │
│  ✓ Request Routing      ✓ Load Balancing                    │
└──┬──────┬──────┬──────┬──────┬──────┬──────────────────────┘
   │      │      │      │      │      │
   │      │      │      │      │      └─────────────┐
   │      │      │      │      │                    │
   ▼      ▼      ▼      ▼      ▼                    ▼
┌─────┐┌─────┐┌─────┐┌─────┐┌─────┐┌─────┐   ┌──────────┐
│Users││Prod.││Rest.││Order││Deliv││Notif│   │ Eureka   │
│8082 ││8083 ││8084 ││8085 ││8086 ││8087 │   │  8761    │
└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘└──┬──┘   └──────────┘
   │      │      │      │      │      │
   │      │      │      │      │      │
   ▼      ▼      ▼      ▼      ▼      ▼
┌─────────────────────────────────────────────────────────────┐
│              ⚡ EVENT BUS (Apache Kafka - 9092)              │
│  Topics: orders.created, orders.confirmed,                  │
│         deliveries.assigned, payments.completed             │
└─────────────────────────────────────────────────────────────┘
   │      │      │      │      │      │
   ▼      ▼      ▼      ▼      ▼      ▼
┌─────────────────────────────────────────────────────────────┐
│                    💾 DATA LAYER                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │PostgreSQL│  │ MongoDB  │  │  Redis   │  │Zookeeper │   │
│  │  5432    │  │  27017   │  │  6379    │  │  2181    │   │
│  │          │  │          │  │          │  │          │   │
│  │• Users   │  │• Restau. │  │• Cache   │  │• Kafka   │   │
│  │• Products│  │• Delivery│  │• Session │  │  Coord.  │   │
│  │• Orders  │  │          │  │          │  │          │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Services & Their Roles

### Infrastructure Services

| Service | Port | Role | Technology |
|---------|------|------|------------|
| **Eureka Server** | 8761 | Service Discovery | Netflix Eureka |
| **API Gateway** | 8080 | Single Entry Point | Spring Cloud Gateway |
| **PostgreSQL** | 5432 | Relational Database | PostgreSQL 16 |
| **MongoDB** | 27017 | NoSQL Database | MongoDB 7.0 |
| **Redis** | 6379 | Cache Layer | Redis 7 |
| **Kafka** | 9092 | Event Streaming | Apache Kafka 3.6 |
| **Zookeeper** | 2181 | Kafka Coordination | Apache Zookeeper 3.9 |

### Business Services

| Service | Port | Database | Purpose | Communication |
|---------|------|----------|---------|---------------|
| **Users** | 8082 | PostgreSQL | Auth + User Profiles | REST (sync) |
| **Products** | 8083 | PostgreSQL + Redis | Product Catalog | REST + Kafka (consumer) |
| **Restaurants** | 8084 | MongoDB | Restaurant Management | REST (sync) |
| **Orders** | 8085 | PostgreSQL | Order Processing | REST + Kafka (producer) |
| **Delivery** | 8086 | MongoDB | Delivery Logistics | Kafka (consumer) + REST |
| **Notification** | 8087 | None | Multi-channel Alerts | Kafka (consumer) |

---

## 🔄 Communication Patterns

### 1. Synchronous Communication (REST API)

```
Client → Gateway → Microservice → Database → Response
```

**Use Cases:**
- User login (immediate response needed)
- Product search (low latency required)
- Get order status

**Example:**
```bash
GET http://localhost:8080/api/v1/products
Authorization: Bearer <JWT_TOKEN>
```

### 2. Asynchronous Communication (Kafka Events)

```
Orders Service → Kafka Topic → [Delivery Service, Notification Service]
```

**Use Cases:**
- Order creation → Delivery assignment
- Payment success → Send receipt
- Order status change → Notify customer

**Event Flow:**
```
1. User creates order
2. Orders Service saves to DB
3. Orders Service publishes ORDER_CREATED event
4. Delivery Service consumes event → assigns driver
5. Notification Service consumes event → sends email/SMS
```

---

## 🐳 Docker Architecture

### Complete Stack in One Command

```bash
docker-compose up -d
```

**What Starts:**
- ✅ 7 Infrastructure containers (databases, Kafka, Redis)
- ✅ 8 Application containers (Gateway + 6 microservices + Eureka)
- ✅ 1 Private network (beefood-network)
- ✅ 3 Persistent volumes (PostgreSQL, MongoDB, Redis data)

### Service Dependencies

```
Infrastructure Services (Postgres, MongoDB, Redis, Kafka, Zookeeper)
   ↓ [depends_on with health checks]
Eureka Server (Service Discovery)
   ↓ [depends_on]
API Gateway (Routes requests to services)
   ↓ [depends_on]
Microservices (Users, Products, Restaurants, Orders, Delivery, Notification)
```

### Health Check Flow

Each service has health checks to ensure dependencies are ready:

```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8082/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 5
```

---

## 🔐 Security Architecture

### JWT Authentication Flow

```
1. User → POST /api/v1/auth/login → Users Service
2. Users Service validates credentials
3. Users Service issues JWT (access + refresh tokens)
4. Client stores JWT
5. Client → GET /api/v1/orders (with JWT in header)
6. Gateway validates JWT
7. Gateway extracts user info (userId, roles)
8. Gateway forwards request with X-User-Id header
9. Orders Service trusts X-User-Id (no re-validation)
```

**JWT Contains:**
- User ID
- Email
- Roles (CUSTOMER, RESTAURANT_OWNER, DRIVER, ADMIN)
- Expiration time (24 hours)

---

## 📊 Data Architecture

### Database-per-Service Pattern

**Why?**
- Each service owns its data (no shared databases)
- Services are independently scalable
- No database-level coupling
- Different databases for different use cases

### Data Distribution

**PostgreSQL (Relational - ACID Transactions):**
- `user_service_db` → Users + Auth credentials
- `product_service_db` → Products + Inventory
- `order_service_db` → Orders + Payments

**MongoDB (Document - Flexible Schema):**
- `restaurant_service_db` → Restaurants + Menus
- `delivery_service_db` → Deliveries + Driver locations

**Redis (Cache - In-Memory):**
- Product cache (TTL: 10 min)
- Menu cache (TTL: 5 min)
- Search results (TTL: 5 min)

---

## 🚀 Typical Request Flows

### Flow 1: User Places Order

```
1. Client → POST /api/v1/orders (with JWT)
2. Gateway → Validates JWT → Routes to Orders Service
3. Orders Service → Validates order data
4. Orders Service → Checks Products Service (REST call) for inventory
5. Orders Service → Processes payment
6. Orders Service → Saves order to PostgreSQL
7. Orders Service → Publishes ORDER_CREATED event to Kafka
8. Delivery Service → Consumes event → Assigns driver
9. Delivery Service → Publishes DELIVERY_ASSIGNED event
10. Notification Service → Consumes events → Sends email + SMS
11. Orders Service → Returns order confirmation to client
```

### Flow 2: Get Product Details (with Cache)

```
1. Client → GET /api/v1/products/123 (with JWT)
2. Gateway → Validates JWT → Routes to Products Service
3. Products Service → Checks Redis cache
4. If cache HIT → Return product from Redis (1ms latency)
5. If cache MISS → Query PostgreSQL → Store in Redis → Return
```

---

## ⚡ Performance Optimizations

### 1. Caching Strategy
- **What:** Popular products, menus, search results
- **Where:** Redis
- **TTL:** 5-10 minutes
- **Impact:** 60-80% reduction in database load

### 2. Asynchronous Processing
- **What:** Order processing, delivery assignment, notifications
- **How:** Kafka events
- **Impact:** Non-blocking, improved response times

### 3. Load Balancing
- **What:** Multiple instances of same service
- **How:** Eureka + Gateway client-side load balancing
- **Impact:** Horizontal scalability

### 4. Database Optimization
- **Indexes:** On frequently queried fields
- **Connection Pooling:** HikariCP (default in Spring Boot)
- **Query Optimization:** N+1 prevention, eager/lazy loading

---

## 🛠️ Technology Stack

### Backend
- **Framework:** Spring Boot 3.2+
- **Language:** Java 21
- **Build Tool:** Maven 3.8+

### Microservices
- **API Gateway:** Spring Cloud Gateway
- **Service Discovery:** Netflix Eureka
- **REST API:** Spring Web MVC
- **Data Access:** Spring Data JPA, Spring Data MongoDB
- **Cache:** Spring Data Redis
- **Messaging:** Spring Kafka

### Databases
- **Relational:** PostgreSQL 16
- **NoSQL:** MongoDB 7.0
- **Cache:** Redis 7

### Infrastructure
- **Event Streaming:** Apache Kafka 3.6 + Zookeeper 3.9
- **Containerization:** Docker 20.10+
- **Orchestration:** Docker Compose 2.0+

### Security
- **Authentication:** JWT (JSON Web Tokens)
- **Authorization:** Spring Security + Role-Based Access Control

---

## 📈 Scalability Considerations

### Horizontal Scaling

```bash
# Scale Orders Service to 3 instances
docker-compose up -d --scale order-service=3
```

**Automatic:**
- Load balancing via Gateway + Eureka
- Kafka consumer groups (parallel processing)

### Vertical Scaling

Increase resources in `docker-compose.yml`:

```yaml
deploy:
  resources:
    limits:
      cpus: '2'
      memory: 2G
```

### Database Scaling

**PostgreSQL:**
- Read replicas for read-heavy workloads
- Partitioning for large tables

**MongoDB:**
- Sharding for horizontal partitioning
- Replica sets for high availability

**Redis:**
- Redis Cluster for distributed caching

---

## 🔍 Monitoring & Observability

### Current Implementation
- **Health Checks:** Spring Boot Actuator
- **Service Discovery:** Eureka Dashboard (http://localhost:8761)
- **Container Logs:** `docker-compose logs -f`

### Future Enhancements
- **Metrics:** Prometheus + Grafana
- **Distributed Tracing:** Zipkin or Jaeger
- **Centralized Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)
- **APM:** New Relic or Datadog

---

## 🎓 Key Learning Outcomes

By studying BeeFood, you'll understand:

1. ✅ **Microservices Architecture** - Service decomposition, boundaries, independence
2. ✅ **Event-Driven Architecture** - Asynchronous communication with Kafka
3. ✅ **API Gateway Pattern** - Centralized routing, auth, and load balancing
4. ✅ **Service Discovery** - Dynamic service registration and lookup
5. ✅ **Database-per-Service** - Data isolation and independence
6. ✅ **Containerization** - Docker for consistency and portability
7. ✅ **Cloud-Native Design** - Scalability, fault tolerance, observability
8. ✅ **Security** - JWT authentication and authorization
9. ✅ **Caching** - Performance optimization with Redis
10. ✅ **Testing** - Unit, integration, and end-to-end tests

---

## 📝 Quick Command Reference

```bash
# Build all services
./scripts/build-all.sh

# Start everything
docker-compose up -d

# View logs (all services)
docker-compose logs -f

# View logs (specific service)
docker-compose logs -f user-service

# Stop all services
docker-compose down

# Clean restart (removes data)
docker-compose down -v && docker-compose up -d

# Check service status
docker-compose ps

# Scale service
docker-compose up -d --scale order-service=3

# Rebuild service
docker-compose up -d --build user-service
```

---

## 🗂️ Project Structure

```
BeeFood/
├── api-gateway/                 # Spring Cloud Gateway (8080)
├── discovery-service/           # Eureka Server (8761)
├── user-service/               # Users + Auth (8082)
├── product-service/            # Products (8083)
├── restaurant-service/         # Restaurants (8084)
├── order-service/             # Orders (8085)
├── delivery-service/          # Delivery (8086)
├── notification-service/      # Notifications (8087)
├── scripts/
│   ├── build-all.sh           # Build all services
│   └── init-databases.sh      # Initialize databases
├── docker-compose.yml         # Complete orchestration
├── docker-compose-infrastructure.yml  # Infrastructure only
├── README.md                  # Project overview
├── ARCHITECTURE.md           # Detailed architecture guide
├── DOCKER_QUICKSTART.md     # Quick start with Docker
├── Instruction.md           # Installation & configuration
└── .github/
    └── copilot-instructions.md  # AI coding guidelines
```

---

## 🎯 Next Steps

1. **Quick Start:** Follow [DOCKER_QUICKSTART.md](DOCKER_QUICKSTART.md)
2. **Deep Dive:** Read [ARCHITECTURE.md](ARCHITECTURE.md)
3. **Customize:** Refer to [Instruction.md](Instruction.md)
4. **Develop:** Check [.github/copilot-instructions.md](.github/copilot-instructions.md)

---

**BeeFood - Production-ready microservices platform for food delivery 🚀🐝**
