#!/bin/bash

# =============================================================================
# BeeFood Platform - Run Services Locally with Containerized Infrastructure
# =============================================================================
# This script helps you run microservices locally while using Docker containers
# for databases and infrastructure (PostgreSQL, MongoDB, Redis, Kafka, Zookeeper)
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get the script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}BeeFood Platform - Local Development Setup${NC}"
echo -e "${BLUE}==============================================================================${NC}"
echo ""

# =============================================================================
# Step 1: Check and start infrastructure containers
# =============================================================================
echo -e "${YELLOW}Step 1: Checking infrastructure containers...${NC}"

# Stop any existing service containers (keep infrastructure)
echo -e "${YELLOW}Stopping any running service containers...${NC}"
docker stop eureka-server api-gateway user-service product-service order-service restaurant-service delivery-service notification-service 2>/dev/null || true

# Check if infrastructure containers are running
REQUIRED_CONTAINERS=("beefood-postgres" "beefood-mongodb" "beefood-redis" "beefood-kafka" "beefood-zookeeper")
MISSING_CONTAINERS=()

for container in "${REQUIRED_CONTAINERS[@]}"; do
    if ! docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        MISSING_CONTAINERS+=("$container")
    fi
done

if [ ${#MISSING_CONTAINERS[@]} -gt 0 ]; then
    echo -e "${YELLOW}Starting infrastructure containers: ${MISSING_CONTAINERS[*]}${NC}"
    cd "$PROJECT_ROOT"
    docker-compose up -d postgres mongodb redis zookeeper kafka
    
    echo -e "${YELLOW}Waiting for infrastructure to be ready (30 seconds)...${NC}"
    sleep 30
else
    echo -e "${GREEN}✓ All infrastructure containers are running${NC}"
fi

# Wait for health checks
echo -e "${YELLOW}Verifying infrastructure health...${NC}"
sleep 5

# Check health status
docker ps --filter "name=beefood-" --format "table {{.Names}}\t{{.Status}}"

echo ""

# =============================================================================
# Step 2: Display connection information
# =============================================================================
echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}Infrastructure Connection Details${NC}"
echo -e "${BLUE}==============================================================================${NC}"
echo -e "${GREEN}PostgreSQL:${NC}"
echo "  Host: localhost:5432"
echo "  Username: admin"
echo "  Password: admin123"
echo "  Databases: user_service_db, product_service_db, order_service_db"
echo ""
echo -e "${GREEN}MongoDB:${NC}"
echo "  Host: localhost:27017"
echo "  Username: admin"
echo "  Password: admin123"
echo ""
echo -e "${GREEN}Redis:${NC}"
echo "  Host: localhost:6379"
echo ""
echo -e "${GREEN}Kafka:${NC}"
echo "  Bootstrap Servers: localhost:9092"
echo ""
echo -e "${BLUE}==============================================================================${NC}"
echo ""

# =============================================================================
# Step 3: Instructions for running services
# =============================================================================
echo -e "${BLUE}==============================================================================${NC}"
echo -e "${BLUE}How to Run Services Locally${NC}"
echo -e "${BLUE}==============================================================================${NC}"
echo ""
echo -e "${YELLOW}Open separate terminal tabs and run each service:${NC}"
echo ""
echo -e "${GREEN}1. Discovery Service (Eureka Server) - Port 8761${NC}"
echo "   cd $PROJECT_ROOT/discovery-service"
echo "   mvn spring-boot:run"
echo ""
echo -e "${GREEN}2. API Gateway - Port 8080${NC}"
echo "   cd $PROJECT_ROOT/api-gateway"
echo "   mvn spring-boot:run"
echo ""
echo -e "${GREEN}3. User Service - Port 8082${NC}"
echo "   cd $PROJECT_ROOT/user-service"
echo "   mvn spring-boot:run"
echo ""
echo -e "${GREEN}4. Product Service - Port 8083${NC}"
echo "   cd $PROJECT_ROOT/product-service"
echo "   mvn spring-boot:run"
echo ""
echo -e "${GREEN}5. Restaurant Service - Port 8084${NC}"
echo "   cd $PROJECT_ROOT/restaurant-service"
echo "   mvn spring-boot:run"
echo ""
echo -e "${GREEN}6. Order Service - Port 8085${NC}"
echo "   cd $PROJECT_ROOT/order-service"
echo "   mvn spring-boot:run"
echo ""
echo -e "${GREEN}7. Delivery Service - Port 8086${NC}"
echo "   cd $PROJECT_ROOT/delivery-service"
echo "   mvn spring-boot:run"
echo ""
echo -e "${GREEN}8. Notification Service - Port 8087${NC}"
echo "   cd $PROJECT_ROOT/notification-service"
echo "   mvn spring-boot:run"
echo ""
echo -e "${BLUE}==============================================================================${NC}"
echo ""
echo -e "${YELLOW}Recommended Order:${NC}"
echo "  1. Start Discovery Service first (wait until fully started)"
echo "  2. Start API Gateway second (wait until registered with Eureka)"
echo "  3. Start remaining services in any order"
echo ""
echo -e "${YELLOW}Access Points:${NC}"
echo "  • Eureka Dashboard: http://localhost:8761"
echo "  • API Gateway: http://localhost:8080"
echo "  • Services: Register with Eureka and accessible via Gateway"
echo ""
echo -e "${YELLOW}To stop infrastructure:${NC}"
echo "  docker-compose down"
echo ""
echo -e "${GREEN}Ready! Now start your services in separate terminals.${NC}"
echo -e "${BLUE}==============================================================================${NC}"
