package com.syndica.api.domain.dtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenDTO(
    @NotBlank(message = "Refresh token is required")
    String refreshToken
) {}
