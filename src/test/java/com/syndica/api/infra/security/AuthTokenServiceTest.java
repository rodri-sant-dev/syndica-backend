package com.syndica.api.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.syndica.api.domain.models.RefreshToken;
import com.syndica.api.domain.models.Role;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.RefreshTokenRepository;
import com.syndica.api.domain.repositories.UserRoleRepository;
import com.syndica.api.infra.Execptions.UnauthorizedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class AuthTokenServiceTest {
    private static final String SECRET = "unit-test-secret-key-with-at-least-32-characters";

    private RefreshTokenRepository refreshTokenRepository;
    private UserRoleRepository userRoleRepository;
    private AuthTokenService authTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        authTokenService = new AuthTokenService(
            SECRET,
            "syndica-api-test",
            Duration.ofMinutes(15),
            Duration.ofDays(3),
            refreshTokenRepository,
            userRoleRepository
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
        when(userRoleRepository.findByUser(user)).thenReturn(List.of());

        String token = authTokenService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(parseClaims(token).get("authorities", List.class)).isEmpty();
    }

    @Test
    void includesDistinctUserRoleNamesInAccessToken() {
        Role admin = Role.builder().name("admin").build();
        Role seller = Role.builder().name("seller").build();
        Role duplicateAdmin = Role.builder().name("admin").build();
        when(userRoleRepository.findByUser(user)).thenReturn(List.of(
            com.syndica.api.domain.models.UserRole.builder().user(user).role(admin).build(),
            com.syndica.api.domain.models.UserRole.builder().user(user).role(seller).build(),
            com.syndica.api.domain.models.UserRole.builder().user(user).role(duplicateAdmin).build()
        ));

        String token = authTokenService.generateAccessToken(user);

        assertThat(parseClaims(token).get("authorities", List.class))
            .containsExactly("admin", "seller");
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

    @Test
    void rotatesValidRefreshTokenAndRevokesTheOriginal() {
        when(userRoleRepository.findByUser(user)).thenReturn(List.of());
        RefreshToken currentToken = RefreshToken.builder()
            .user(user)
            .tokenHash("current-hash")
            .expiresAt(null)
            .revoked(false)
            .build();
        RefreshToken replacement = RefreshToken.builder()
            .user(user)
            .tokenHash("replacement-hash")
            .revoked(false)
            .build();

        when(refreshTokenRepository.findByTokenHash(anyString()))
            .thenReturn(java.util.Optional.of(currentToken))
            .thenReturn(java.util.Optional.of(replacement));

        var tokens = authTokenService.rotateRefreshToken("current-token");

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).hasSize(43);
        assertThat(currentToken.isRevoked()).isTrue();
        assertThat(currentToken.getReplaceMotive()).isEqualTo("ROTATED");
        assertThat(currentToken.getReplacedByToken()).isSameAs(replacement);
        verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rejectsUnknownRefreshToken() {
        when(refreshTokenRepository.findByTokenHash(anyString()))
            .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authTokenService.rotateRefreshToken("unknown-token"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Refresh token is invalid");
    }

    @Test
    void rejectsExpiredRefreshToken() {
        RefreshToken expiredToken = RefreshToken.builder()
            .user(user)
            .tokenHash("expired-hash")
            .expiresAt(Instant.now().minusSeconds(1))
            .revoked(false)
            .build();
        when(refreshTokenRepository.findByTokenHash(anyString()))
            .thenReturn(java.util.Optional.of(expiredToken));

        assertThatThrownBy(() -> authTokenService.rotateRefreshToken("expired-token"))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessage("Refresh token is invalid");
    }

    @Test
    void revokesRefreshTokenOnLogout() {
        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .tokenHash("refresh-hash")
            .expiresAt(Instant.now().plusSeconds(60))
            .revoked(false)
            .build();
        when(refreshTokenRepository.findByTokenHash(anyString()))
            .thenReturn(java.util.Optional.of(refreshToken));

        authTokenService.revokeRefreshToken("refresh-token");

        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(refreshToken.getReplaceMotive()).isEqualTo("LOGOUT");
        verify(refreshTokenRepository).save(refreshToken);
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer("syndica-api-test")
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    @Test
    void logoutIsIdempotentForUnknownOrAlreadyRevokedTokens() {
        RefreshToken revokedToken = RefreshToken.builder()
            .user(user)
            .tokenHash("refresh-hash")
            .revoked(true)
            .build();
        when(refreshTokenRepository.findByTokenHash(anyString()))
            .thenReturn(java.util.Optional.empty())
            .thenReturn(java.util.Optional.of(revokedToken));

        authTokenService.revokeRefreshToken("unknown-token");
        authTokenService.revokeRefreshToken("revoked-token");

        org.mockito.Mockito.verify(refreshTokenRepository, org.mockito.Mockito.never())
            .save(any(RefreshToken.class));
    }
}
