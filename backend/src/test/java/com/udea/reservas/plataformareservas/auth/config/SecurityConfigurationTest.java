package com.udea.reservas.plataformareservas.auth.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

class SecurityConfigurationTest {

    @Test
    void authenticationManagerAuthenticatesValidCredentials() {
        // Arrange
        SecurityConfiguration configuration = new SecurityConfiguration();
        UserDetailsService userDetailsService = username ->
            User.withUsername(username)
                .password("secret")
                .roles("USER")
                .build();
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString();
            }

            @Override
            public boolean matches(
                CharSequence rawPassword,
                String encodedPassword
            ) {
                return rawPassword.toString().equals(encodedPassword);
            }
        };

        // Act
        AuthenticationManager authenticationManager =
            configuration.authenticationManager(
                userDetailsService,
                passwordEncoder
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
        assertTrue(repository instanceof HttpSessionSecurityContextRepository);
    }

}
