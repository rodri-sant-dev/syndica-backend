package com.syndica.api.domain.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syndica.api.domain.models.User;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);
}
