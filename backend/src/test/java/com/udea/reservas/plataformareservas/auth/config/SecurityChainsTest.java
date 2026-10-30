package com.udea.reservas.plataformareservas.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.udea.reservas.plataformareservas.TestAuthentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

class SecurityChainsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AccessDeniedHandler handler =
        SecurityChains.unauthenticatedAs401(new JsonSecurityErrorHandler(objectMapper));

    @Test
    void csrfRejectionWithoutASessionRespondsUnauthorized() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest("POST", "/x"), response, new AccessDeniedException("x"));

        assertEquals(401, response.getStatus());
    }

    @Test
    void csrfRejectionWithAnAuthenticatedSessionRespondsForbidden() throws Exception {
        TestAuthentication.authenticateAs(TestAuthentication.principal(1));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest("DELETE", "/x"), response, new AccessDeniedException("x"));

        assertEquals(403, response.getStatus());
    }
}