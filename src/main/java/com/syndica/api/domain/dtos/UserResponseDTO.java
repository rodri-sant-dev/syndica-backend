package com.syndica.api.domain.dtos;

import lombok.Builder;

@Builder
public record UserResponseDTO(
    Integer id,
    String username,
    String email
) {}
