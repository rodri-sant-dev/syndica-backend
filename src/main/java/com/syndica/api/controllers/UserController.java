package com.syndica.api.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syndica.api.domain.dtos.UserDTO;
import com.syndica.api.domain.dtos.UserMeResponseDTO;
import com.syndica.api.domain.dtos.UserResponseDTO;
import com.syndica.api.domain.models.User;
import com.syndica.api.mappers.UserMapper;
import com.syndica.api.services.UserService;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> create(
        @Valid @RequestBody UserDTO userDTO,
        @AuthenticationPrincipal User userAuth
    ) {
        User user = userService.create(userDTO);
        return ResponseEntity.status(201).body(UserMapper.toResponse(user));
    }

    @GetMapping("/me")
    public UserMeResponseDTO me(@AuthenticationPrincipal User userAuth) {
        return UserMapper.toMeResponse(
            userAuth,
            userService.getGroups(userAuth.getId())
        );
    }

    @PatchMapping("/{id}/activate")
    public UserResponseDTO activate(@PathVariable UUID id) {
        return UserMapper.toResponse(userService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public UserResponseDTO deactivate(
        @PathVariable UUID id,
        @AuthenticationPrincipal User userAuth
    ) {
        return UserMapper.toResponse(userService.deactivate(id, userAuth.getId()));
    }
}
