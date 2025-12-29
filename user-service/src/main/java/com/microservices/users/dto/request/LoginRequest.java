package com.microservices.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "BLANK_FIELD")
    @Email(message = "INVALID_EMAIL")
    private String email;

    @NotBlank(message = "BLANK_FIELD")
    private String password;
}