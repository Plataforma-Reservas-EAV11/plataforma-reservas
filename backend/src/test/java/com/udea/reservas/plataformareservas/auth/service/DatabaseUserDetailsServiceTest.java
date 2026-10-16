package com.udea.reservas.plataformareservas.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.usuarios.model.User;
import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

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
        verify(userRepository).findByEmailIgnoreCase("ana@example.com");
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
