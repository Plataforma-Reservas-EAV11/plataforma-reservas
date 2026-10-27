package com.udea.reservas.plataformareservas.auth.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.udea.reservas.plataformareservas.TestAuthentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityChainsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "HEAD", "TRACE", "OPTIONS" })
    void safeMethodsDoNotRequireCsrf(String method) {
        assertFalse(SecurityChains.isUnsafeMethod(new MockHttpServletRequest(method, "/x")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
    void writeMethodsRequireCsrf(String method) {
        assertTrue(SecurityChains.isUnsafeMethod(new MockHttpServletRequest(method, "/x")));
    }

    @Test
    void csrfIsNotRequiredWithoutASession() {
        SecurityContextHolder.clearContext();

        assertFalse(SecurityChains.isUnsafeMethodWithSession(new MockHttpServletRequest("POST", "/x")));
    }

    @Test
    void csrfIsRequiredForWritesWithAnAuthenticatedSession() {
        TestAuthentication.authenticateAs(TestAuthentication.principal(1));

        assertTrue(SecurityChains.isUnsafeMethodWithSession(new MockHttpServletRequest("DELETE", "/x")));
        assertFalse(SecurityChains.isUnsafeMethodWithSession(new MockHttpServletRequest("GET", "/x")));
    }
}
