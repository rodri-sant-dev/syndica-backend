package com.syndica.api.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.syndica.api.domain.dtos.UserDTO;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.mappers.UserMapper;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(UserDTO userDTO) {
        String encodedPassword = passwordEncoder.encode(userDTO.password());
        return userRepository.save(UserMapper.toEntity(userDTO, encodedPassword));
    }
}
