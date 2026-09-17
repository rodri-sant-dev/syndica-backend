package com.syndica.api.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syndica.api.domain.dtos.LoginDTO;
import com.syndica.api.domain.dtos.TokensDTO;
import com.syndica.api.services.LoginService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("token/")
public class TokenController {  
    private final LoginService loginService;

    public TokenController(
        LoginService loginService
    ){
        this.loginService = loginService;
    }

    @PostMapping("login/")
    public TokensDTO login(@Valid @RequestBody LoginDTO loginDTO) {
        return loginService.getTokens(
            loginService.getUserOfCredentials(loginDTO),
            loginDTO.remember()
        );
    }    
}