package com.udea.reservas.plataformareservas.auth.service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Identidad autenticada que viaja en la sesión. Los accesores (getId, getName,
 * getEmail, getRole, getPassword e isAccountNonLocked) los genera Lombok; las
 * autoridades se derivan de {@code role}. Las banderas de cuenta no expirada,
 * credenciales vigentes y cuenta habilitada usan los valores por defecto de
 * {@link UserDetails} (siempre verdaderos).
 */
@Getter
@AllArgsConstructor
public final class UserPrincipal implements UserDetails, CredentialsContainer {

    private final Integer id;
    private final String name;
    private final String email;
    private final String role;
    private String password;
    private final boolean accountNonLocked;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        return List.of(
            new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT))
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public void eraseCredentials() {
        password = null;
    }
}
