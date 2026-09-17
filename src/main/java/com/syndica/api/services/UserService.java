package com.syndica.api.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.syndica.api.domain.dtos.UserDTO;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.RefreshTokenRepository;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.infra.Execptions.ConflictException;
import com.syndica.api.infra.Execptions.NotFoundException;
import com.syndica.api.mappers.UserMapper;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public User create(UserDTO userDTO) {
        String encodedPassword = passwordEncoder.encode(userDTO.password());
        return userRepository.save(UserMapper.toEntity(userDTO, encodedPassword));
    }

    public User activate(Integer userId) {
        User user = getById(userId);
        user.setActive(true);
        return userRepository.save(user);
    }

    public User deactivate(Integer userId, Integer requesterId) {
        if (userId.equals(requesterId)) {
            throw new ConflictException("A manager cannot deactivate their own user");
        }

        User user = getById(userId);
        user.setActive(false);
        var activeTokens = refreshTokenRepository.findByUserAndRevokedFalse(user);
        activeTokens.forEach(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshToken.setReplaceMotive("USER_DEACTIVATED");
        });
        refreshTokenRepository.saveAll(activeTokens);
        return userRepository.save(user);
    }

    public User getById(Integer userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
