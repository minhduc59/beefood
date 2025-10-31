package com.microservices.gateway.filter;


import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RouteValidator {

    // Public endpoints (no authentication)
    public static final Map<String, List<HttpMethod>> openApiEndpoints = new HashMap<>() {{
        put("/api/v1/auth", List.of(HttpMethod.POST)); // login/register
        put("/api/v1/restaurants", List.of(HttpMethod.GET));
        put("/api/v1/products", List.of(HttpMethod.GET));
    }};

    // Customer endpoints
    public static final Map<String, List<HttpMethod>> customerEndpoints = new HashMap<>() {{
        put("/api/v1/orders", List.of(HttpMethod.POST, HttpMethod.GET));
        put("/api/v1/deliveries", List.of(HttpMethod.GET));
    }};

    // Restaurant Owner endpoints
    public static final Map<String, List<HttpMethod>> restaurantOwnerEndpoints = new HashMap<>() {{
        put("/api/v1/restaurants", List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));
        put("/api/v1/products", List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));
        put("/api/v1/orders", List.of(HttpMethod.GET)); // view restaurant orders
    }};

    // Delivery Driver endpoints
    public static final Map<String, List<HttpMethod>> deliveryEndpoints = new HashMap<>() {{
        put("/api/v1/deliveries", List.of(HttpMethod.PUT, HttpMethod.GET));
    }};

    // Admin endpoints
    public static final Map<String, List<HttpMethod>> adminEndpoints = new HashMap<>() {{
        put("/api/v1/admin", List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));
    }};

    // Helper: Check if the request matches any endpoint in map
    public boolean securityFilter(ServerHttpRequest request, Map<String, List<HttpMethod>> endpoints) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        return endpoints.entrySet().stream()
                .noneMatch(entry ->
                        path.contains(entry.getKey()) &&
                                entry.getValue().contains(method)
                );
    }
}
