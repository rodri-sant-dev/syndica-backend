package com.syndica.api.domain.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
    @NotBlank(message="Email is required")
    String email,
    @NotBlank(message="Password is required")
    String password,
    boolean remember
){}