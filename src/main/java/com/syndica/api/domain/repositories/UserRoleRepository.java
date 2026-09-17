package com.syndica.api.domain.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syndica.api.domain.models.User;
import com.syndica.api.domain.models.UserRole;
import com.syndica.api.domain.models.Role;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUser(User user);

    boolean existsByUserAndRole(User user, Role role);

    Optional<UserRole> findByUserAndRole(User user, Role role);
}
