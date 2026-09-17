package com.syndica.api.domain.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syndica.api.domain.models.User;
import com.syndica.api.domain.models.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUser(User user);
}
