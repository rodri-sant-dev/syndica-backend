package com.syndica.api.services;

import org.springframework.stereotype.Service;

import com.syndica.api.domain.models.Role;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.models.UserRole;
import com.syndica.api.domain.repositories.RoleRepository;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.domain.repositories.UserRoleRepository;
import com.syndica.api.infra.Execptions.ConflictException;
import com.syndica.api.infra.Execptions.NotFoundException;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public RoleService(
        RoleRepository roleRepository,
        UserRepository userRepository,
        UserRoleRepository userRoleRepository
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public Role create(String name, String description) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new ConflictException("Role already exists");
        }

        return roleRepository.save(Role.builder()
            .name(name)
            .description(description)
            .build());
    }

    public UserRole addUserToRole(String email, String roleName) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found"));
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new NotFoundException("Role not found"));

        if (userRoleRepository.existsByUserAndRole(user, role)) {
            throw new ConflictException("User already has this role");
        }

        return userRoleRepository.save(UserRole.builder()
            .user(user)
            .role(role)
            .build());
    }

    public void removeUserFromRole(String email, String roleName) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found"));
        Role role = roleRepository.findByName(roleName)
            .orElseThrow(() -> new NotFoundException("Role not found"));
        UserRole userRole = userRoleRepository.findByUserAndRole(user, role)
            .orElseThrow(() -> new NotFoundException("User does not have this role"));

        userRoleRepository.delete(userRole);
    }
}
