package com.syndica.api.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.syndica.api.domain.models.RefreshToken;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.RefreshTokenRepository;

class AuthTokenServiceTest {
    private static final String SECRET = "unit-test-secret-key-with-at-least-32-characters";

    private RefreshTokenRepository refreshTokenRepository;
    private AuthTokenService authTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        authTokenService = new AuthTokenService(
            SECRET,
            "syndica-api-test",
            Duration.ofMinutes(15),
            Duration.ofDays(3),
            refreshTokenRepository
        );
        user = User.builder()
            .id(1)
            .username("test-user")
            .email("test@example.com")
            .password("encoded-password")
            .build();
    }

    @Test
    void generatesAccessToken() {
        String token = authTokenService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void generates256BitRefreshTokenAndPersistsOnlyItsHash() {
        String token = authTokenService.createRefreshToken(user, false);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertThat(token).hasSize(43);
        assertThat(savedToken.getTokenHash()).hasSize(43);
        assertThat(savedToken.getTokenHash()).isNotEqualTo(token);
        assertThat(savedToken.getExpiresAt()).isNotNull();
    }

    @Test
    void doesNotSetExpirationWhenRememberIsTrue() {
        authTokenService.createRefreshToken(user, true);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        assertThat(captor.getValue().getExpiresAt()).isNull();
    }
}
