package com.udea.reservas.plataformareservas.auth.service;

import com.udea.reservas.plataformareservas.usuarios.model.User;
import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final Clock clock;

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() ->
                new UsernameNotFoundException("Invalid credentials.")
            );

        return new UserPrincipal(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getPasswordHash(),
            !user.isLockedAt(LocalDateTime.now(clock))
        );
    }
}
