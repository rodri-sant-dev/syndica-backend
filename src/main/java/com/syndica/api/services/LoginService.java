package com.syndica.api.services;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.syndica.api.domain.dtos.LoginDTO;
import com.syndica.api.domain.dtos.TokensDTO;
import com.syndica.api.domain.models.User;
import com.syndica.api.domain.repositories.UserRepository;
import com.syndica.api.infra.execptions.NotFoundException;

@Service
public class LoginService {
    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

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
        .orElseThrow(() -> {
            log.warn("Login failed: user not found email={}", credentials.email());
            return new NotFoundException("email or password is incorrect");
        });
        
        if (!passwordEncoder.matches(credentials.password(), user.getPassword())) {
            log.warn("Login failed: invalid password userId={}", user.getId());
            throw new NotFoundException("email or password is incorrect");
        }
        if (!user.isActive()) {
            log.warn("Login failed: inactive user userId={}", user.getId());
            throw new NotFoundException("email or password is incorrect");
        }

        log.info("Login succeeded userId={}", user.getId());
        return user;
    }

    public TokensDTO getTokens(User user, boolean remember){
        log.info("Issuing tokens userId={} remember={}", user.getId(), remember);
        return new TokensDTO(
            authTokenService.generateAccessToken(user),
            authTokenService.createRefreshToken(user, remember)
        );
    }

    public TokensDTO refreshTokens(String refreshToken) {
        log.info("Refreshing tokens");
        return authTokenService.rotateRefreshToken(refreshToken);
    }

    public void logout(String refreshToken) {
        log.info("Logging out");
        authTokenService.revokeRefreshToken(refreshToken);
    }
}
