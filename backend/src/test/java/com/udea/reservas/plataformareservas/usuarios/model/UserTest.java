package com.udea.reservas.plataformareservas.usuarios.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 15, 10, 0);

    @Test
    void newUserHasNoFailedAttemptsAndIsNotLocked() {
        User user = User.builder().build();

        assertEquals(0, user.getFailedLoginAttempts());
        assertFalse(user.isLockedAt(AHORA));
    }

    @Test
    void isLockedOnlyWhileTheLockDateIsInTheFuture() {
        User user = User.builder().lockedUntil(AHORA).build();

        assertTrue(user.isLockedAt(AHORA.minusSeconds(1)));
        assertFalse(user.isLockedAt(AHORA));
        assertFalse(user.isLockedAt(AHORA.plusSeconds(1)));
    }
}
