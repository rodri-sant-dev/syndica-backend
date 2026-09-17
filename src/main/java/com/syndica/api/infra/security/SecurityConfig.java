package com.syndica.api.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/token/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/users").hasAuthority("MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/users/*/activate").hasAuthority("MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/users/*/deactivate").hasAuthority("MANAGER")
                .anyRequest().permitAll())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
