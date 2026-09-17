package com.syndica.api.infra.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import com.syndica.api.domain.dtos.TokensDTO;
import com.syndica.api.domain.models.RefreshToken;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.RefreshTokenRepository;
import com.syndica.api.domain.repositories.UserRoleRepository;
import com.syndica.api.infra.Execptions.UnauthorizedException;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;

@Service
public class AuthTokenService {
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRoleRepository userRoleRepository;
    private final SecureRandom secureRandom;

    public AuthTokenService(
        @Value("${security.jwt.secret}") String secret,
        @Value("${security.jwt.issuer}") String issuer,
        @Value("${security.jwt.access-token-expiration}") Duration accessTokenExpiration,
        @Value("${security.jwt.refresh-token-expiration}") Duration refreshTokenExpiration,
        RefreshTokenRepository refreshTokenRepository,
        UserRoleRepository userRoleRepository
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRoleRepository = userRoleRepository;
        this.secureRandom = new SecureRandom();
    }

    public String generateAccessToken(User user) {
        ensureActive(user);
        Instant issuedAt = Instant.now();

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("authorities", userRoleRepository.findByUser(user).stream()
                .map(userRole -> userRole.getRole())
                .filter(Objects::nonNull)
                .map(role -> role.getName())
                .filter(Objects::nonNull)
                .distinct()
                .toList())
            .issuer(issuer)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plus(accessTokenExpiration)))
            .signWith(signingKey)
            .compact();
    }

    public String createRefreshToken(User user, boolean remember) {
        ensureActive(user);
        Instant expiresAt = remember ? null : Instant.now().plus(refreshTokenExpiration);
        return createRefreshToken(user, expiresAt);
    }

    @Transactional
    public TokensDTO rotateRefreshToken(String token) {
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(hashToken(token))
            .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));

        Instant now = Instant.now();
        if (currentToken.isRevoked()
            || (currentToken.getExpiresAt() != null && !currentToken.getExpiresAt().isAfter(now))) {
            throw new UnauthorizedException("Refresh token is invalid");
        }
        ensureActive(currentToken.getUser());

        boolean remember = currentToken.getExpiresAt() == null;
        String newRefreshToken = createRefreshToken(currentToken.getUser(), remember);
        RefreshToken replacement = refreshTokenRepository.findByTokenHash(hashToken(newRefreshToken))
            .orElseThrow(() -> new IllegalStateException("Generated refresh token was not persisted"));

        currentToken.setRevoked(true);
        currentToken.setReplaceMotive("ROTATED");
        currentToken.setReplacedByToken(replacement);
        refreshTokenRepository.save(currentToken);

        return new TokensDTO(
            generateAccessToken(currentToken.getUser()),
            newRefreshToken
        );
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByTokenHash(hashToken(token))
            .filter(refreshToken -> !refreshToken.isRevoked())
            .ifPresent(refreshToken -> {
                refreshToken.setRevoked(true);
                refreshToken.setReplaceMotive("LOGOUT");
                refreshTokenRepository.save(refreshToken);
            });
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private void ensureActive(User user) {
        if (!user.isActive()) {
            throw new UnauthorizedException("email or password is incorrect");
        }
    }

    private String createRefreshToken(User user, Instant expiresAt) {
        byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);

        String token = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(tokenBytes);

        Instant now = Instant.now();
        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .tokenHash(hashToken(token))
            .createdAt(now)
            .expiresAt(expiresAt)
            .revoked(false)
            .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
