package com.syndica.api.domain.repositories;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syndica.api.domain.models.RefreshToken;
import com.syndica.api.domain.models.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserAndRevokedFalse(User user);
}
