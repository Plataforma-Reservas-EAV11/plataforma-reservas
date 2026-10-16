package com.udea.reservas.plataformareservas.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class UserPrincipalTest {

    @Test
    void principalExposesIdentityAndAccountStatus() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
            7,
            "Ana",
            "ana@example.com",
            "hashed-secret"
        );

        // Act
        Collection<? extends GrantedAuthority> authorities =
            principal.getAuthorities();

        // Assert
        assertEquals(7, principal.getId());
        assertEquals("Ana", principal.getName());
        assertEquals("ana@example.com", principal.getEmail());
        assertEquals("ana@example.com", principal.getUsername());
        assertEquals("hashed-secret", principal.getPassword());
        assertTrue(authorities.isEmpty());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
    }

    @Test
    void eraseCredentialsRemovesPassword() {
        // Arrange
        UserPrincipal principal = new UserPrincipal(
            7,
            "Ana",
            "ana@example.com",
            "hashed-secret"
        );

        // Act
        principal.eraseCredentials();

        // Assert
        assertNull(principal.getPassword());
    }
}
