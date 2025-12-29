# 🚀 BeeFood - Quick Start with Docker

This guide will help you quickly get the entire BeeFood platform running with a single command.

## Prerequisites

- Docker Desktop installed and running
- Docker Compose v2.0+
- At least 8GB RAM available
- Ports 5432, 6379, 8080-8087, 9092, 27017 available

## Quick Start (3 Steps)

### Step 1: Build All Services

```bash
# Make the build script executable (first time only)
chmod +x scripts/build-all.sh

# Build all microservices
./scripts/build-all.sh
```

This will compile all 8 microservices and create JAR files.

### Step 2: Start Everything

```bash
# Start all infrastructure + microservices
docker-compose up -d
```

This single command will start:
- ✅ PostgreSQL (3 databases)
- ✅ MongoDB (2 databases)
- ✅ Redis cache
- ✅ Kafka + Zookeeper
- ✅ Eureka Server (Service Discovery)
- ✅ API Gateway (Port 8080)
- ✅ 6 Microservices (Users, Products, Restaurants, Orders, Delivery, Notification)

### Step 3: Verify Services

```bash
# Check all services are running
docker-compose ps

# View Eureka Dashboard
open http://localhost:8761

# View API Gateway health
curl http://localhost:8080/actuator/health
```

**Expected Output**: All services should show as `Up` and registered in Eureka.

---

## Usage Examples

### 1. Register a New User

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@beefood.com",
    "password": "Test123!",
    "role": "CUSTOMER"
  }'
```

### 2. Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@beefood.com",
    "password": "Test123!"
  }'
```

**Save the JWT token** from the response.

### 3. Get Products (with JWT)

```bash
export TOKEN="your_jwt_token_here"

curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Create an Order

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

---

## Management Commands

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f user-service

# Last 100 lines
docker-compose logs --tail=100 order-service
```

### Restart a Service

```bash
# Restart after code changes
docker-compose restart user-service

# Rebuild and restart
docker-compose up -d --build user-service
```

### Stop All Services

```bash
# Stop (keep data)
docker-compose stop

# Stop and remove containers (keep data)
docker-compose down

# Stop and remove everything including data (⚠️ WARNING)
docker-compose down -v
```

### Scale Services

```bash
# Run 3 instances of Orders Service
docker-compose up -d --scale order-service=3
```

---

## Monitoring

### Service Discovery

Visit **Eureka Dashboard**: http://localhost:8761

You should see all services registered:
- API-GATEWAY
- users
- products
- restaurants
- orders
- deliveries
- notifications

### Database Access

**PostgreSQL**:
```bash
docker-compose exec postgres psql -U admin -d user_service_db
```

**MongoDB**:
```bash
docker-compose exec mongodb mongosh mongodb://admin:admin123@localhost:27017
```

**Redis**:
```bash
docker-compose exec redis redis-cli
```

### Kafka Topics

```bash
# List topics
docker-compose exec kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 --list

# Consume messages
docker-compose exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic orders.created \
  --from-beginning
```

---

## Development Workflow

### Option 1: Full Docker (Recommended for Testing)

```bash
# Start everything
docker-compose up -d

# Make code changes in any service
# Rebuild specific service
docker-compose up -d --build user-service

# View logs
docker-compose logs -f user-service
```

### Option 2: Hybrid (Recommended for Development)

```bash
# Start only infrastructure
docker-compose up -d postgres mongodb redis kafka zookeeper eureka-server

# Run services locally for faster development
cd user-service
mvn spring-boot:run

# In another terminal
cd product-service
mvn spring-boot:run
```

---

## Troubleshooting

### Services Not Starting

```bash
# Check service status
docker-compose ps

# Check logs for errors
docker-compose logs <service-name>

# Common fixes:
# 1. Ensure all services were built
./scripts/build-all.sh

# 2. Ensure ports are available
lsof -i :8080

# 3. Restart Docker Desktop
```

### Database Connection Issues

```bash
# Wait for databases to be healthy
docker-compose ps

# Restart dependent service
docker-compose restart user-service
```

### Port Already in Use

```bash
# Find and kill process
lsof -i :8080
kill -9 <PID>

# Or change port in docker-compose.yml
ports:
  - "8081:8080"  # Use 8081 instead
```

---

## Clean Restart

```bash
# Complete clean restart
docker-compose down -v
./scripts/build-all.sh
docker-compose up -d
```

---

## Next Steps

After the platform is running:

1. **Explore APIs**: Check `ARCHITECTURE.md` for detailed API documentation
2. **Add Swagger**: Integrate Swagger UI for interactive API docs
3. **Set Up Monitoring**: Add Prometheus + Grafana
4. **Write Tests**: Create integration tests
5. **Deploy to Cloud**: Migrate to Kubernetes

---

## Quick Reference

| Service | Port | Purpose |
|---------|------|---------|
| API Gateway | 8080 | Single entry point |
| Eureka | 8761 | Service discovery |
| Users | 8082 | Auth + user management |
| Products | 8083 | Product catalog |
| Restaurants | 8084 | Restaurant management |
| Orders | 8085 | Order processing |
| Delivery | 8086 | Delivery logistics |
| Notification | 8087 | Notifications |
| PostgreSQL | 5432 | Relational DB |
| MongoDB | 27017 | NoSQL DB |
| Redis | 6379 | Cache |
| Kafka | 9092 | Event streaming |

---

## Support

- **Architecture Guide**: See `ARCHITECTURE.md`
- **Installation Guide**: See `Instruction.md`
- **AI Agent Instructions**: See `.github/copilot-instructions.md`

---

**Happy Coding! 🚀🐝**
