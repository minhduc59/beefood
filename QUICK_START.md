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
```

## 📋 Service Ports

| Service              | Port | Description                    |
|---------------------|------|--------------------------------|
| Discovery (Eureka)  | 8761 | Service registry               |
| API Gateway         | 8080 | Single entry point             |
| User Service        | 8082 | User profiles & authentication |
| Product Service     | 8083 | Food & beverage inventory      |
| Restaurant Service  | 8084 | Restaurant profiles & menus    |
| Order Service       | 8085 | Order processing               |
| Delivery Service    | 8086 | Delivery logistics             |
| Notification Service| 8087 | Notifications (email/SMS)      |

## 🗄️ Infrastructure

| Component   | Port  | Credentials              | Used By                           |
|-------------|-------|--------------------------|-----------------------------------|
| PostgreSQL  | 5432  | admin/admin123           | User, Product, Order Services     |
| MongoDB     | 27017 | admin/admin123           | Restaurant, Delivery Services     |
| Redis       | 6379  | No password              | Product Service (caching)         |
| Kafka       | 9092  | No auth                  | All services (event streaming)    |
| Zookeeper   | 2181  | No auth                  | Kafka coordination                |

## 🎯 Important URLs

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway Health**: http://localhost:8080/actuator/health
- **Access Services**: Through Gateway (port 8080)

## 🛠️ Common Commands

### Check Infrastructure Status
```bash
docker ps --filter "name=beefood-"
```

### View Logs
```bash
docker-compose logs -f postgres mongodb kafka
```

### Connect to Databases
```bash
# PostgreSQL
docker exec -it beefood-postgres psql -U admin -d user_service_db

# MongoDB
docker exec -it beefood-mongodb mongosh -u admin -p admin123

# Redis
docker exec -it beefood-redis redis-cli
```

### Monitor Kafka Topics
```bash
# List topics
docker exec -it beefood-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume messages
docker exec -it beefood-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic orders.created \
  --from-beginning
```

### Stop Everything
```bash
# Stop services: Ctrl+C in each terminal

# Stop infrastructure
docker-compose down

# Stop and remove data
docker-compose down -v
```

## 🐛 Troubleshooting

### Port Already in Use
```bash
# Find and kill process
lsof -i :8761
kill -9 <PID>

# Or stop Docker container
docker stop eureka-server
```

### Service Won't Start
1. Check if infrastructure is healthy: `docker ps`
2. Check if Eureka is running: http://localhost:8761
3. Check service logs for errors
4. Verify correct startup order

### Database Connection Failed
```bash
# Restart database containers
docker-compose restart postgres mongodb
sleep 10
```

## ✅ Verification Checklist

- [ ] All infrastructure containers show "healthy" status
- [ ] Discovery Service started and accessible at port 8761
- [ ] API Gateway registered in Eureka dashboard
- [ ] All business services registered in Eureka
- [ ] Can access Gateway health endpoint

## 📝 Development Tips

1. **Start in Order**: Discovery → Gateway → Services
2. **Check Eureka**: Always verify services registered
3. **One Service**: Only run services you're working on
4. **Hot Reload**: Use Spring Boot DevTools for auto-reload
5. **Debug Mode**: Attach debugger to any service in your IDE

---

For detailed instructions, see [LOCAL_DEVELOPMENT.md](LOCAL_DEVELOPMENT.md)
