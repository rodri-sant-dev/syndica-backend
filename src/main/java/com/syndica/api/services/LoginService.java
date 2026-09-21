package com.syndica.api.services;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.syndica.api.domain.dtos.LoginDTO;
import com.syndica.api.domain.dtos.TokensDTO;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.infra.execptions.NotFoundException;

@Service
public class LoginService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;

    public LoginService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthTokenService authTokenService
    ){
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.authTokenService = authTokenService;
    }

    public User getUserOfCredentials(LoginDTO credentials){
        User user = userRepository.findByEmail(credentials.email())
        .orElseThrow(() -> new NotFoundException("email or password is incorrect"));
        
        if (!passwordEncoder.matches(credentials.password(), user.getPassword())) {
            throw new NotFoundException("email or password is incorrect");
        }
        if (!user.isActive()) {
            throw new NotFoundException("email or password is incorrect");
        }

        return user;
    }

    public TokensDTO getTokens(User user, boolean remember){
        return new TokensDTO(
            authTokenService.generateAccessToken(user),
            authTokenService.createRefreshToken(user, remember)
        );
    }

    public TokensDTO refreshTokens(String refreshToken) {
        return authTokenService.rotateRefreshToken(refreshToken);
    }

    public void logout(String refreshToken) {
        authTokenService.revokeRefreshToken(refreshToken);
    }
}
