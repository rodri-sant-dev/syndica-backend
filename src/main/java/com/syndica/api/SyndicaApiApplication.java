package com.syndica.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.syndica.api.domain.dtos.UserDTO;
import com.syndica.api.domain.models.Role;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.domain.repositories.RoleRepository;
import com.syndica.api.domain.models.User;
import com.syndica.api.services.RoleService;
import com.syndica.api.services.UserService;

@SpringBootApplication
public class SyndicaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SyndicaApiApplication.class, args);
	}

	@Bean
	CommandLineRunner localUserInitializer(
		UserRepository userRepository,
		UserService userService,
		RoleRepository roleRepository,
		RoleService roleService
	) {
		return args -> {
			Role managerRole = roleRepository.findByName("SINDICO")
				.orElseGet(() -> roleService.create("SINDICO", "condominium manager"));
			long userCount = userRepository.count();
			
			if (userCount == 0) {
				User user = userService.create(new UserDTO(
					"local-user",
					"admin@admin.com",
					"Admin@Admin1!"
				));
				roleService.addUserToRole(user.getEmail(), managerRole.getName());
			}
		};
	}
}
