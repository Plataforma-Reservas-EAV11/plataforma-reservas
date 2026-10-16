package com.udea.reservas.plataformareservas.auth.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderConfigurationTest {

    @Test
    void passwordEncoderCreatesBcryptEncoder() {
        // Arrange
        PasswordEncoderConfiguration configuration =
            new PasswordEncoderConfiguration();

        // Act
        PasswordEncoder passwordEncoder = configuration.passwordEncoder();

        // Assert
        assertTrue(passwordEncoder instanceof BCryptPasswordEncoder);
        assertTrue(passwordEncoder.matches(
            "secret",
            passwordEncoder.encode("secret")
        ));
    }
}
