package com.udea.reservas.plataformareservas.auth.service;

import static org.mockito.Mockito.verify;

import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 15, 10, 0);

    @Mock
    private UserRepository userRepository;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        Clock relojFijo = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        service = new LoginAttemptService(userRepository, relojFijo);
    }

    @Test
    void recordFailureCountsTheAttemptAndLocksWhenTheLimitIsReached() {
        service.recordFailure("ana@example.com");

        verify(userRepository).incrementFailedLoginAttempts("ana@example.com");
        verify(userRepository).lockWhenFailedAttemptsReach(
            "ana@example.com",
            5,
            AHORA.plusMinutes(15)
        );
    }

    @Test
    void recordSuccessClearsTheAttempts() {
        service.recordSuccess("ana@example.com");

        verify(userRepository).clearFailedLoginAttempts("ana@example.com");
    }
}
