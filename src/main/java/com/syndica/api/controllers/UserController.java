package com.syndica.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syndica.api.domain.dtos.UserDTO;
import com.syndica.api.domain.dtos.UserResponseDTO;
import com.syndica.api.domain.models.User;
import com.syndica.api.mappers.UserMapper;
import com.syndica.api.services.UserService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserDTO userDTO) {
        User user = userService.create(userDTO);
        return ResponseEntity.status(201).body(UserMapper.toResponse(user));
    }

    @PatchMapping("/{id}/activate")
    public UserResponseDTO activate(@PathVariable Integer id) {
        return UserMapper.toResponse(userService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public UserResponseDTO deactivate(
        @PathVariable Integer id,
        Authentication authentication
    ) {
        Integer requesterId = Integer.valueOf(authentication.getName());
        return UserMapper.toResponse(userService.deactivate(id, requesterId));
    }
}
