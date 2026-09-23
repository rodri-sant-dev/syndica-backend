package com.syndica.api.services;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.syndica.api.domain.dtos.UserDTO;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.RefreshTokenRepository;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.domain.repositories.UserRoleRepository;
import com.syndica.api.infra.execptions.ConflictException;
import com.syndica.api.infra.execptions.NotFoundException;
import com.syndica.api.mappers.UserMapper;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRoleRepository userRoleRepository;

    public UserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        RefreshTokenRepository refreshTokenRepository,
        UserRoleRepository userRoleRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public User create(UserDTO userDTO) {
        String encodedPassword = passwordEncoder.encode(userDTO.password());
        User user = userRepository.save(UserMapper.toEntity(userDTO, encodedPassword));
        log.info("User created userId={}", user.getId());
        return user;
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User activate(UUID userId) {
        User user = getById(userId);
        user.setActive(true);
        User activatedUser = userRepository.save(user);
        log.info("User activated userId={}", userId);
        return activatedUser;
    }

    public User deactivate(UUID userId, UUID requesterId) {
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
        User deactivatedUser = userRepository.save(user);
        log.info(
            "User deactivated userId={} revokedRefreshTokens={}",
            userId,
            activeTokens.size()
        );
        return deactivatedUser;
    }

    public User getById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public List<String> getGroups(UUID userId) {
        return userRoleRepository.findRoleNamesByUserId(userId);
    }
}
