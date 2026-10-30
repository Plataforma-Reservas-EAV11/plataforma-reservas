package com.udea.reservas.plataformareservas.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * DEF-01: reservas y agendas solo se pueden usar con la sesión creada por
 * {@code POST /api/v1/auth/login}. La identidad sale de esa sesión (los
 * controladores reciben el {@code UserPrincipal}), nunca del cuerpo ni de la URL.
 * Sin sesión responde 401; una sesión sin token CSRF válido en POST/DELETE, 403.
 */
@Configuration
public class ApiSecurityConfiguration {

    @Bean
    @Order(2)
    public SecurityFilterChain protectedApiFilterChain(
        HttpSecurity http,
        SecurityContextRepository securityContextRepository,
        JsonSecurityErrorHandler errorHandler
    ) {
        http
            .securityMatcher(
                "/api/reservas",
                "/api/reservas/**",
                "/api/agendas",
                "/api/agendas/**"
            )
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());

        return SecurityChains.build(
            http,
            securityContextRepository,
            errorHandler,
            SecurityChains.unauthenticatedAs401(errorHandler)
        );
    }
}
