package com.syndica.api.infra.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.syndica.api.domain.repositories.RefreshTokenRepository;
import com.syndica.api.domain.repositories.UserRoleRepository;
import com.syndica.api.services.AuthTokenService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;

class JwtAuthenticationFilterTest {
    private static final String SECRET = "unit-test-secret-key-with-at-least-32-characters";

    @AfterEach
    void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void ignoresAccessTokenWithInvalidUuidSubject() throws Exception {
        AuthTokenService authTokenService = new AuthTokenService(
            SECRET,
            "syndica-api-test",
            java.time.Duration.ofMinutes(15),
            java.time.Duration.ofDays(3),
            mock(RefreshTokenRepository.class),
            mock(UserRoleRepository.class)
        );
        ConfigUserDetailsService userDetailsService = mock(ConfigUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            authTokenService,
            userDetailsService
        );
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenWithSubject("not-a-uuid"));

        filter.doFilter(
            request,
            new MockHttpServletResponse(),
            filterChain
        );

        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(
            org.mockito.ArgumentMatchers.eq(request),
            org.mockito.ArgumentMatchers.any()
        );
    }

    private String tokenWithSubject(String subject) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        return Jwts.builder()
            .subject(subject)
            .issuer("syndica-api-test")
            .issuedAt(now)
            .expiration(new Date(now.getTime() + 60_000))
            .signWith(key)
            .compact();
    }
}
