package com.microservices.users.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "BLANK_FIELD")
    @Size(min = 1, max = 50, message = "OUT_OF_SIZE")
    private String firstName;

    @Size(min = 0, max = 50, message = "OUT_OF_SIZE")
    private String middleName;

    @NotBlank(message = "BLANK_FIELD")
    @Size(min = 1, max = 50, message = "OUT_OF_SIZE")
    private String lastName;

    @NotBlank(message = "BLANK_FIELD")
    @Email(message = "INVALID_EMAIL")
    private String email;

    @NotBlank(message = "BLANK_FIELD")
    private String role;
}
