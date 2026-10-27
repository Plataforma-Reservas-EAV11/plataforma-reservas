package com.udea.reservas.plataformareservas.auth.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LoginRequestTest {

    @Test
    void emailIsTrimmedAndLowercased() {
        assertEquals("ana@example.com", new LoginRequest("  Ana@Example.COM ", "x").email());
    }

    @Test
    void nullEmailIsKeptSoValidationCanReportIt() {
        assertNull(new LoginRequest(null, "x").email());
    }
}
