echo "Building all BeeFood microservices..."

services=("discovery-service" "api-gateway" "user-service" "product-service" "restaurant-service" "order-service" "delivery-service" "notification-service")

for service in "${services[@]}"; do
    echo "Building $service..."
    cd $service
    mvn clean install -DskipTests
    cd ..
done

echo "All services built successfully!"