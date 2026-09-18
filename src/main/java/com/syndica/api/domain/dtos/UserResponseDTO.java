package com.syndica.api.domain.dtos;

import java.util.UUID;

import lombok.Builder;

@Builder
public record UserResponseDTO(
    UUID id,
    String username,
    String email,
    boolean active
) {}
