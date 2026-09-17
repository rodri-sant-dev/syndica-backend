package com.syndica.api.infra.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.domain.repositories.UserRoleRepository;

@Service
public class ConfigUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public ConfigUserDetailsService(
        UserRepository userRepository,
        UserRoleRepository userRoleRepository
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toUserDetails(user);
    }

    public UserDetails loadUserById(Integer userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toUserDetails(user);
    }

    private UserDetails toUserDetails(User user) {
        String[] authorities = userRoleRepository.findRoleNamesByUserId(user.getId()).stream()
            .filter(role -> role != null && !role.isBlank())
            .distinct()
            .toArray(String[]::new);

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getId().toString())
            .password(user.getPassword())
            .authorities(authorities)
            .disabled(!user.isActive())
            .build();
    }
}