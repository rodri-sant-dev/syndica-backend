package com.syndica.api.infra.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.syndica.api.domain.models.RefreshToken;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.RefreshTokenRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class AuthTokenService {
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom;

    public AuthTokenService(
        @Value("${security.jwt.secret}") String secret,
        @Value("${security.jwt.issuer}") String issuer,
        @Value("${security.jwt.access-token-expiration}") Duration accessTokenExpiration,
        @Value("${security.jwt.refresh-token-expiration}") Duration refreshTokenExpiration,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.refreshTokenRepository = refreshTokenRepository;
        this.secureRandom = new SecureRandom();
    }

    public String generateAccessToken(User user) {
        Instant issuedAt = Instant.now();

        return Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .issuer(issuer)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plus(accessTokenExpiration)))
            .signWith(signingKey)
            .compact();
    }

    public String createRefreshToken(User user, boolean remember) {
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
            .expiresAt(remember ? null : now.plus(refreshTokenExpiration))
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
