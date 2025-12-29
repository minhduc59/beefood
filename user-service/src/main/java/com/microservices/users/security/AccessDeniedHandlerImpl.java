package com.microservices.users.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.users.dto.response.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.error("Access denied: {} - {}", request.getRequestURI(), accessDeniedException.getMessage());

        APIResponse<?> apiResponse = APIResponse.builder()
                .message("Access Denied: You do not have the necessary permissions to access this resource.")
                .build();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403 status code

        new ObjectMapper().writeValue(response.getOutputStream(), apiResponse);
    }
}