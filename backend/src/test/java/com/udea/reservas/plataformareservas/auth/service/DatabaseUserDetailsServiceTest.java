package com.udea.reservas.plataformareservas.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.usuarios.model.User;
import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 15, 10, 0);

    @Mock
    private UserRepository userRepository;

    private DatabaseUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        Clock relojFijo = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        userDetailsService = new DatabaseUserDetailsService(userRepository, relojFijo);
    }

    @ParameterizedTest
    @ValueSource(strings = { "usuario", "proveedor" })
    void loadUserByUsernameReturnsPrincipalWithPersistedRole(String role) {
        // Arrange
        User user = User.builder()
            .id(7)
            .name("Ana")
            .email("ana@example.com")
            .passwordHash("hashed-secret")
            .role(role)
            .build();
        when(userRepository.findByEmailIgnoreCase("ana@example.com"))
            .thenReturn(Optional.of(user));

        // Act
        UserPrincipal result = (UserPrincipal) userDetailsService
            .loadUserByUsername("  ANA@EXAMPLE.COM ");

        // Assert
        assertEquals(7, result.getId());
        assertEquals("Ana", result.getName());
        assertEquals("ana@example.com", result.getEmail());
        assertEquals(role, result.getRole());
        assertEquals("hashed-secret", result.getPassword());
        assertTrue(result.isAccountNonLocked());
        verify(userRepository).findByEmailIgnoreCase("ana@example.com");
    }

    @Test
    void loadUserByUsernameMarksTheAccountAsLockedWhileTheLockIsActive() {
        // Arrange
        User user = User.builder()
            .id(7)
            .name("Ana")
            .email("ana@example.com")
            .passwordHash("hashed-secret")
            .lockedUntil(AHORA.plusMinutes(5))
            .build();
        when(userRepository.findByEmailIgnoreCase("ana@example.com"))
            .thenReturn(Optional.of(user));

        // Act
        UserPrincipal result = (UserPrincipal) userDetailsService
            .loadUserByUsername("ana@example.com");

        // Assert
        assertFalse(result.isAccountNonLocked());
    }

    @Test
    void loadUserByUsernameUnlocksTheAccountOnceTheLockExpires() {
        // Arrange
        User user = User.builder()
            .id(7)
            .name("Ana")
            .email("ana@example.com")
            .passwordHash("hashed-secret")
            .lockedUntil(AHORA.minusMinutes(1))
            .build();
        when(userRepository.findByEmailIgnoreCase("ana@example.com"))
            .thenReturn(Optional.of(user));

        // Act
        UserPrincipal result = (UserPrincipal) userDetailsService
            .loadUserByUsername("ana@example.com");

        // Assert
        assertTrue(result.isAccountNonLocked());
    }

    @Test
    void loadUserByUsernameThrowsWhenEmailDoesNotExist() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("missing@example.com"))
            .thenReturn(Optional.empty());

        // Act
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("missing@example.com")
        );

        // Assert
        assertEquals("Invalid credentials.", exception.getMessage());
        verify(userRepository).findByEmailIgnoreCase("missing@example.com");
    }
}
