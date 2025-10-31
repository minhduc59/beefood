package com.microservices.gateway.filter;



import com.microservices.gateway.exception.CustomException;
import com.microservices.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final RouteValidator validator;
    private final JwtUtil jwtUtil;

    public AuthenticationFilter(RouteValidator validator, JwtUtil jwtUtil) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Skip open endpoints
            if (!validator.securityFilter(request, RouteValidator.openApiEndpoints)) {
                return chain.filter(exchange);
            }

            // Check token
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                throw new CustomException("Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new CustomException("Invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);
            jwtUtil.validateToken(token);

            // Validate user role access
            validateRole(token, request);

            return chain.filter(exchange);
        };
    }

    private void validateRole(String token, ServerHttpRequest request) {
        Claims claims = jwtUtil.getClaims(token);
        String role = (String) claims.get("role");

        switch (role) {
            case "ADMIN" -> checkAccess(request, RouteValidator.adminEndpoints, role);
            case "RESTAURANT_OWNER" -> checkAccess(request, RouteValidator.restaurantOwnerEndpoints, role);
            case "CUSTOMER" -> checkAccess(request, RouteValidator.customerEndpoints, role);
            case "DELIVERY_DRIVER" -> checkAccess(request, RouteValidator.deliveryEndpoints, role);
            default -> throw new CustomException("Unknown role: " + role, HttpStatus.FORBIDDEN);
        }
    }
    //
    private void checkAccess(ServerHttpRequest request, Map<String, List<HttpMethod>> endpoints, String role) {
        boolean allowed = endpoints.entrySet().stream()
                .anyMatch(entry -> request.getURI().getPath().contains(entry.getKey())
                        && entry.getValue().contains(request.getMethod()));

        if (!allowed) {
            log.error("{} role is not authorized to access {}", role, request.getURI().getPath());
            throw new CustomException(role + " role not authorized for this action", HttpStatus.FORBIDDEN);
        }
    }

    public static class Config {
    }
}
