package com.udea.reservas.plataformareservas.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class JsonSecurityErrorHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSecurityErrorHandler handler = new JsonSecurityErrorHandler(objectMapper);

    @Test
    void commenceRespondsUnauthorizedJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), response, new BadCredentialsException("x"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("UNAUTHENTICATED", body.get("errorCode").asString());
        assertEquals(
            "Debes iniciar sesión para realizar esta operación.",
            body.get("message").asString()
        );
    }

    @Test
    void handleRespondsForbiddenJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("x"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, response.getStatus());
        assertEquals("CSRF_INVALID", body.get("errorCode").asString());
        assertEquals("La solicitud no pudo validarse.", body.get("message").asString());
    }
}
