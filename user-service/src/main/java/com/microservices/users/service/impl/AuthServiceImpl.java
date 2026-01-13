package com.microservices.users.service.impl;

import com.microservices.users.dto.request.LoginRequest;
import com.microservices.users.dto.response.LoginResponse;
import com.microservices.users.entity.User;
import com.microservices.users.mapper.UserMapper;
import com.microservices.users.repository.UserRepository;
import com.microservices.users.security.JwtTokenProvider;
import com.microservices.users.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        // Step 1: Authenticate user (validates password)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        // Step 2: Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Step 3: Fetch user from database
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginRequest.getEmail()));

        // Step 4: Generate JWT token
        String jwt = tokenProvider.generateToken(user.getEmail());

        log.info("Login successful for user: {}", user.getEmail());

        // Step 5: Build and return response with UserMapper
        return LoginResponse.of(jwt, user, userMapper);
    }
}