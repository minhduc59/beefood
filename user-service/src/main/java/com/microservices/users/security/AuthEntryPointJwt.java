package com.microservices.users.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.users.dto.response.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.error("Unauthorized access attempt: {} - {}", request.getRequestURI(), authException.getMessage());

        APIResponse<?> apiResponse = APIResponse.builder()
                .message("Unauthorized: " + authException.getMessage())
                .build();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401 status code

        new ObjectMapper().writeValue(response.getOutputStream(), apiResponse);
    }
}