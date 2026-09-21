package com.syndica.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.syndica.api.domain.models.Role;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.models.UserRole;
import com.syndica.api.domain.repositories.RoleRepository;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.domain.repositories.UserRoleRepository;
import com.syndica.api.infra.execptions.ConflictException;
import com.syndica.api.infra.execptions.NotFoundException;

class RoleServiceTest {
    private RoleRepository roleRepository;
    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RoleService roleService;
    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        roleRepository = org.mockito.Mockito.mock(RoleRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        userRoleRepository = org.mockito.Mockito.mock(UserRoleRepository.class);
        roleService = new RoleService(roleRepository, userRepository, userRoleRepository);
        user = User.builder()
            .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            .email("user@example.com")
            .username("user")
            .build();
        role = Role.builder()
            .id(1)
            .name("admin")
            .description("Administrator")
            .build();
    }

    @Test
    void createsRole() {
        when(roleRepository.findByName("admin")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        Role created = roleService.create("admin", "Administrator");

        assertThat(created).isSameAs(role);
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void rejectsDuplicateRole() {
        when(roleRepository.findByName("admin")).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> roleService.create("admin", "Administrator"))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Role already exists");
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void addsUserToRole() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(role.getName())).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserAndRole(user, role)).thenReturn(false);
        UserRole saved = UserRole.builder().user(user).role(role).build();
        when(userRoleRepository.save(any(UserRole.class))).thenReturn(saved);

        UserRole result = roleService.addUserToRole(user.getEmail(), role.getName());

        assertThat(result).isSameAs(saved);
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void rejectsDuplicateUserRole() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(role.getName())).thenReturn(Optional.of(role));
        when(userRoleRepository.existsByUserAndRole(user, role)).thenReturn(true);

        assertThatThrownBy(() -> roleService.addUserToRole(user.getEmail(), role.getName()))
            .isInstanceOf(ConflictException.class)
            .hasMessage("User already has this role");
        verify(userRoleRepository, never()).save(any(UserRole.class));
    }

    @Test
    void rejectsUnknownUserWhenAddingRole() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.addUserToRole(user.getEmail(), role.getName()))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("User not found");
    }

    @Test
    void removesUserRole() {
        UserRole userRole = UserRole.builder().user(user).role(role).build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(role.getName())).thenReturn(Optional.of(role));
        when(userRoleRepository.findByUserAndRole(user, role)).thenReturn(Optional.of(userRole));

        roleService.removeUserFromRole(user.getEmail(), role.getName());

        verify(userRoleRepository).delete(userRole);
    }

    @Test
    void rejectsRemovingMissingUserRole() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(roleRepository.findByName(role.getName())).thenReturn(Optional.of(role));
        when(userRoleRepository.findByUserAndRole(user, role)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.removeUserFromRole(user.getEmail(), role.getName()))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("User does not have this role");
        verify(userRoleRepository, never()).delete(any(UserRole.class));
    }
}
