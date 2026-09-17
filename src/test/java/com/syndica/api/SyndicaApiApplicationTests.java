package com.syndica.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.syndica.api.domain.models.RefreshToken;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.RefreshTokenRepository;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.infra.security.AuthTokenService;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SyndicaApiApplicationTests {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
		.withDatabaseName("syndica_test")
		.withUsername("test")
		.withPassword("test");

	@Autowired
	private AuthTokenService authTokenService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@DynamicPropertySource
	static void configureDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
	}

	@BeforeEach
	void cleanDatabase() {
		refreshTokenRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createsExpiringRefreshTokenInPostgres() {
		User user = saveUser();

		String token = authTokenService.createRefreshToken(user, false);

		assertThat(token).hasSize(43);
		RefreshToken savedToken = refreshTokenRepository.findAll().getFirst();
		assertThat(savedToken.getUser().getId()).isEqualTo(user.getId());
		assertThat(savedToken.getTokenHash()).hasSize(43);
		assertThat(savedToken.getExpiresAt()).isNotNull();
		assertThat(savedToken.isRevoked()).isFalse();
	}

	@Test
	void createsNonExpiringRefreshTokenWhenRememberIsTrue() {
		User user = saveUser();

		authTokenService.createRefreshToken(user, true);

		RefreshToken savedToken = refreshTokenRepository.findAll().getFirst();
		assertThat(savedToken.getExpiresAt()).isNull();
	}

	private User saveUser() {
		return userRepository.save(User.builder()
			.username("test-user")
			.email("test@example.com")
			.password("encoded-password")
			.build());
	}

}
