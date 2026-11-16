package com.udea.reservas.plataformareservas.auth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.udea.reservas.plataformareservas.TestAuthentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Ramas de {@link SecurityChains#unauthenticatedAs401}; el flujo completo está en CsrfFilterChainBehaviorTest. */
class UnauthenticatedAs401HandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AccessDeniedHandler handler =
        SecurityChains.unauthenticatedAs401(new JsonSecurityErrorHandler(objectMapper));

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void emptyContextRespondsUnauthorizedJson() throws Exception {
        SecurityContextHolder.clearContext();

        MockHttpServletResponse response = rechazar();

        assertUnauthorized(response);
    }

    @Test
    void anonymousAuthenticationRespondsUnauthorizedJson() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new AnonymousAuthenticationToken(
                "clave",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
            )
        );

        MockHttpServletResponse response = rechazar();

        assertUnauthorized(response);
    }

    @Test
    void authenticatedUserKeepsForbiddenJson() throws Exception {
        TestAuthentication.authenticateAs(TestAuthentication.principal(1));

        MockHttpServletResponse response = rechazar();

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("CSRF_INVALID", body.get("errorCode").asString());
        assertEquals("La solicitud no pudo validarse.", body.get("message").asString());
    }

    private MockHttpServletResponse rechazar() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.handle(new MockHttpServletRequest("POST", "/x"), response, new AccessDeniedException("x"));
        return response;
    }

    private void assertUnauthorized(MockHttpServletResponse response) throws Exception {
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals("UNAUTHENTICATED", body.get("errorCode").asString());
        assertEquals(
            "Debes iniciar sesión para realizar esta operación.",
            body.get("message").asString()
        );
    }
}
