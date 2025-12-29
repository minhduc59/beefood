package com.microservices.users.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.microservices.users.entity.User;
import com.microservices.users.mapper.UserMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String token;
    private UserResponse user;

    public static LoginResponse of(String token, User user, UserMapper userMapper) {
        UserResponse userResponse = userMapper.userToUserResponse(user);
        return LoginResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }
}