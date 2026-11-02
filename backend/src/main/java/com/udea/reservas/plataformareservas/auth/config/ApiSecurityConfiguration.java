package com.udea.reservas.plataformareservas.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Endpoints de reservas y agendas protegidos por sesión (DEF-01). La creación
 * de servicios requiere además la autoridad ROLE_PROVEEDOR. La identidad sale
 * de la sesión, nunca del cuerpo ni de la URL. Sin sesión responde 401; una
 * escritura sin token CSRF válido responde 403.
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
                "/api/agendas/**",
                "/api/v1/providers/me/services",
                "/api/v1/providers/me/services/**"
            )
            .authorizeHttpRequests(authorize ->
                authorize
                    .requestMatchers(
                        "/api/v1/providers/me/services",
                        "/api/v1/providers/me/services/**"
                    )
                    .hasRole("PROVEEDOR")
                    .anyRequest()
                    .authenticated()
            );

        return SecurityChains.build(
            http,
            securityContextRepository,
            errorHandler,
            SecurityChains.unauthenticatedAs401(errorHandler)
        );
    }
}
