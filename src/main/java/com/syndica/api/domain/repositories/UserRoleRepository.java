package com.syndica.api.domain.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.syndica.api.domain.models.User;
import com.syndica.api.domain.models.UserRole;
import com.syndica.api.domain.models.Role;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {

    List<UserRole> findByUser(User user);

    @Query("""
        select role.name
        from UserRole userRole
        join userRole.role role
        where userRole.user.id = :userId
        """)
    List<String> findRoleNamesByUserId(@Param("userId") Integer userId);

    boolean existsByUserAndRole(User user, Role role);

    Optional<UserRole> findByUserAndRole(User user, Role role);
}
