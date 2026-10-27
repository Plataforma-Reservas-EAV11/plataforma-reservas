package com.udea.reservas.plataformareservas.auth.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Autenticación (gestor, repositorio de sesión) y cadena de filtros pública de
 * inicio de sesión. La cadena que protege reservas y agendas está en
 * {@link ApiSecurityConfiguration}; ambas comparten {@link SecurityChains}.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    public AuthenticationManager authenticationManager(
        UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder,
        ApplicationEventPublisher eventPublisher
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
            userDetailsService
        );
        provider.setPasswordEncoder(passwordEncoder);
        ProviderManager providerManager = new ProviderManager(provider);
        // Publica los eventos de éxito y de contraseña incorrecta que alimentan
        // el bloqueo por intentos fallidos (LoginAttemptListener).
        providerManager.setAuthenticationEventPublisher(
            new DefaultAuthenticationEventPublisher(eventPublisher)
        );
        return providerManager;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authenticationFilterChain(
        HttpSecurity http,
        SecurityContextRepository securityContextRepository,
        JsonSecurityErrorHandler errorHandler
    ) {
        http
            .securityMatcher("/api/v1/auth/login", "/api/v1/auth/csrf")
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());

        return SecurityChains.build(
            http,
            securityContextRepository,
            errorHandler,
            SecurityChains::isUnsafeMethod
        );
    }
}
