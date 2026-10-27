package com.udea.reservas.plataformareservas.auth.service;

import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bloqueo temporal de la cuenta tras demasiados intentos fallidos de inicio de
 * sesión (DEF-13). Los contadores se guardan en la tabla {@code usuario}; la
 * cuenta bloqueada la rechaza Spring Security antes de comparar la contraseña
 * (ver {@link UserPrincipal#isAccountNonLocked()}).
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public void recordFailure(String email) {
        userRepository.incrementFailedLoginAttempts(email);
        userRepository.lockWhenFailedAttemptsReach(
            email,
            MAX_FAILED_ATTEMPTS,
            LocalDateTime.now(clock).plus(LOCK_DURATION)
        );
    }

    @Transactional
    public void recordSuccess(String email) {
        userRepository.clearFailedLoginAttempts(email);
    }
}
