package com.udea.reservas.plataformareservas.auth.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

class SecurityConfigurationTest {

    private final PasswordEncoder passwordEncoder = new PasswordEncoder() {
        @Override
        public String encode(CharSequence rawPassword) {
            return rawPassword.toString();
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return rawPassword.toString().equals(encodedPassword);
        }
    };

    @Test
    void authenticationManagerAuthenticatesValidCredentials() {
        // Arrange
        SecurityConfiguration configuration = new SecurityConfiguration();
        UserDetailsService userDetailsService = username ->
            User.withUsername(username)
                .password("secret")
                .roles("USER")
                .build();
        List<Object> events = new ArrayList<>();

        // Act
        AuthenticationManager authenticationManager =
            configuration.authenticationManager(
                userDetailsService,
                passwordEncoder,
                events::add
            );

        // Assert
        assertTrue(
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    "ana@example.com",
                    "secret"
                )
            ).isAuthenticated()
        );
        assertTrue(events.stream().anyMatch(AuthenticationSuccessEvent.class::isInstance));
    }

    @Test
    void authenticationManagerPublishesAnEventWhenThePasswordIsWrong() {
        // Arrange
        SecurityConfiguration configuration = new SecurityConfiguration();
        UserDetailsService userDetailsService = username ->
            User.withUsername(username)
                .password("secret")
                .roles("USER")
                .build();
        List<Object> events = new ArrayList<>();
        AuthenticationManager authenticationManager =
            configuration.authenticationManager(
                userDetailsService,
                passwordEncoder,
                events::add
            );

        // Act
        assertThrows(
            BadCredentialsException.class,
            () -> authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    "ana@example.com",
                    "incorrecta"
                )
            )
        );

        // Assert
        assertTrue(
            events.stream().anyMatch(AuthenticationFailureBadCredentialsEvent.class::isInstance)
        );
        assertFalse(events.stream().anyMatch(AuthenticationSuccessEvent.class::isInstance));
    }

    @Test
    void authenticationManagerRejectsALockedAccountEvenWithTheRightPassword() {
        // Arrange
        SecurityConfiguration configuration = new SecurityConfiguration();
        UserDetailsService userDetailsService = username ->
            User.withUsername(username)
                .password("secret")
                .roles("USER")
                .accountLocked(true)
                .build();
        List<Object> events = new ArrayList<>();
        AuthenticationManager authenticationManager =
            configuration.authenticationManager(
                userDetailsService,
                passwordEncoder,
                events::add
            );

        // Act
        assertThrows(
            LockedException.class,
            () -> authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    "ana@example.com",
                    "secret"
                )
            )
        );

        // Assert: una cuenta bloqueada no cuenta como contraseña incorrecta
        assertFalse(
            events.stream().anyMatch(AuthenticationFailureBadCredentialsEvent.class::isInstance)
        );
    }

    @Test
    void securityContextRepositoryCreatesHttpSessionRepository() {
        // Arrange
        SecurityConfiguration configuration = new SecurityConfiguration();

        // Act
        SecurityContextRepository repository =
            configuration.securityContextRepository();

        // Assert
        assertNotNull(repository);
        assertInstanceOf(HttpSessionSecurityContextRepository.class, repository);
    }
}
