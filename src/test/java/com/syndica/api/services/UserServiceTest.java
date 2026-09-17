package com.syndica.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.syndica.api.domain.models.RefreshToken;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.RefreshTokenRepository;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.infra.Execptions.ConflictException;

class UserServiceTest {
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userService = new UserService(
            userRepository,
            mock(PasswordEncoder.class),
            refreshTokenRepository
        );
        user = User.builder()
            .id(2)
            .username("user")
            .email("user@example.com")
            .active(true)
            .build();
    }

    @Test
    void deactivatesUserAndRevokesActiveRefreshTokens() {
        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .revoked(false)
            .build();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserAndRevokedFalse(user))
            .thenReturn(List.of(refreshToken));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.deactivate(user.getId(), 1);

        assertThat(result.isActive()).isFalse();
        assertThat(refreshToken.isRevoked()).isTrue();
        assertThat(refreshToken.getReplaceMotive()).isEqualTo("USER_DEACTIVATED");
        verify(refreshTokenRepository).saveAll(List.of(refreshToken));
        verify(userRepository).save(user);
    }

    @Test
    void preventsManagerFromDeactivatingOwnUser() {
        assertThatThrownBy(() -> userService.deactivate(user.getId(), user.getId()))
            .isInstanceOf(ConflictException.class)
            .hasMessage("A manager cannot deactivate their own user");

        verify(userRepository, never()).findById(any());
    }
}
