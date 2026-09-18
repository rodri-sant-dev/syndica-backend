package com.syndica.api.domain.dtos;

import java.util.List;

import lombok.Builder;

@Builder
public record UserMeResponseDTO(
    String username,
    String email,
    List<String> grupos
) {}
