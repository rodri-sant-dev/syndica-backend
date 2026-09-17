package com.syndica.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.syndica.api.domain.dtos.UserDTO;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.services.UserService;

@SpringBootApplication
public class SyndicaApiApplication {
	private static final Logger logger = LoggerFactory.getLogger(SyndicaApiApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(SyndicaApiApplication.class, args);
	}

	@Bean
	CommandLineRunner localUserInitializer(
		UserRepository userRepository,
		UserService userService
	) {
		return args -> {
			long userCount = userRepository.count();
			logger.info("Users found at startup: {}", userCount);

			if (userCount == 0) {
				userService.create(new UserDTO(
					"local-user",
					"admin@admin.com",
					"Admin@Admin1!"
				));
				logger.info("Created local development user");
			}
		};
	}
}
