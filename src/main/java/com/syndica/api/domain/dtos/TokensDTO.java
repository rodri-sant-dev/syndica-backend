package com.syndica.api.domain.dtos;

public record TokensDTO(
    String accessToken,
    String refreshToken
) {}
