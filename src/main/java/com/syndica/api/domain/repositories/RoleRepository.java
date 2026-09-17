package com.syndica.api.domain.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syndica.api.domain.models.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(String name);
}
