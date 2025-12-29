package com.microservices.users.mapper;

import com.microservices.users.dto.response.UserResponse;
import com.microservices.users.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse userToUserResponse(User user);
}