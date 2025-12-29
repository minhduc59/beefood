package com.microservices.users.service;

import com.microservices.users.dto.request.LoginRequest;
import com.microservices.users.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);

}
