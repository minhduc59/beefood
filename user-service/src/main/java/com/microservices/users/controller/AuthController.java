package com.microservices.users.controller;

import com.microservices.users.dto.request.LoginRequest;
import com.microservices.users.dto.response.APIResponse;
import com.microservices.users.dto.response.LoginResponse;
import com.microservices.users.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public APIResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        LoginResponse loginResponse = authService.login(loginRequest);

        return APIResponse.<LoginResponse>builder()
                .result(loginResponse)
                .message("Login successful")
                .build();
    }
}