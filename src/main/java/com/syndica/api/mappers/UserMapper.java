package com.syndica.api.mappers;

import com.syndica.api.domain.dtos.UserDTO;
import com.syndica.api.domain.dtos.UserResponseDTO;
import com.syndica.api.domain.models.User;

public final class UserMapper {
    private UserMapper() {}

    public static User toEntity(UserDTO userDTO, String encodedPassword) {
        return User.builder()
            .username(userDTO.username())
            .email(userDTO.email())
            .password(encodedPassword)
            .build();
    }

    public static UserResponseDTO toResponse(User user) {
        return UserResponseDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .build();
    }
}
