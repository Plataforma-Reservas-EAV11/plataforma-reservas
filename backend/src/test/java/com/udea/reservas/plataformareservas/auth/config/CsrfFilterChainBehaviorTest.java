package com.udea.reservas.plataformareservas.auth.config;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.auth.controller.UserController;
import com.udea.reservas.plataformareservas.auth.service.LoginAttemptListener;
import com.udea.reservas.plataformareservas.auth.service.LoginAttemptService;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.auth.service.UserService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * S4502: comportamiento real de la protección CSRF de las dos cadenas, con los
 * filtros completos. Los métodos se prueban sobre una ruta de prueba bajo
 * {@code /api/reservas/**} atendida por {@link ProbeController}: la cadena
 * protege la ruta, no cada controlador, así que PUT y PATCH (que hoy no tienen
 * endpoints) quedan cubiertos si se agregan. Los tres resultados posibles son
 * 401 (bloqueada por autenticación), 403 (bloqueada por CSRF) y 204 (superó los
 * filtros y llegó al controlador de prueba).
 */
@WebMvcTest(UserController.class)
@Import({
    SecurityConfiguration.class,
    ApiSecurityConfiguration.class,
    JsonSecurityErrorHandler.class,
    PasswordEncoderConfiguration.class,
    LoginAttemptListener.class,
    CsrfFilterChainBehaviorTest.ProbeController.class
})
@EnableWebSecurity
class CsrfFilterChainBehaviorTest {

    /** Atiende cualquier método en la ruta protegida para poder distinguir "superó la seguridad" (204). */
    @RestController
    @RequestMapping("/api/reservas/ruta-de-prueba")
    static class ProbeController {

        @RequestMapping
        @ResponseStatus(HttpStatus.NO_CONTENT)
        void atender() {
            // Intentionally empty: reaching this handler proves the request passed the security filters.
        }
    }

    private static final String RUTA_PROTEGIDA = "/api/reservas/ruta-de-prueba";
    private static final String LOGIN_BODY =
        "{\"email\":\"ana@example.com\",\"password\":\"clave-segura-1\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private LoginAttemptService loginAttemptService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = { "GET", "HEAD" })
    @DisplayName("Métodos seguros: con sesión no exigen token CSRF y llegan al controlador")
    void safeMethodsWithSessionDoNotRequireCsrf(String method) throws Exception {
        MockHttpSession session = sesionAutenticada(7);

        mockMvc.perform(request(HttpMethod.valueOf(method), RUTA_PROTEGIDA).session(session))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("OPTIONS con sesión no exige token CSRF (la respuesta 200 la da Spring MVC, no el controlador)")
    void optionsWithSessionDoesNotRequireCsrf() throws Exception {
        mockMvc.perform(request(HttpMethod.OPTIONS, RUTA_PROTEGIDA).session(sesionAutenticada(7)))
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = { "GET", "HEAD", "OPTIONS" })
    @DisplayName("Métodos seguros: sin sesión se rechaza por falta de autenticación, no por CSRF")
    void safeMethodsWithoutSessionRespondUnauthorized(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), RUTA_PROTEGIDA))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TRACE nunca llega a la cadena: el firewall de Spring Security lo rechaza con 400")
    void traceIsRejectedByTheFirewallBeforeTheChain() throws Exception {
        mockMvc.perform(request(HttpMethod.TRACE, RUTA_PROTEGIDA)).andExpect(status().isBadRequest());
        mockMvc.perform(request(HttpMethod.TRACE, RUTA_PROTEGIDA).session(sesionAutenticada(7)))
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
    @DisplayName("Escritura anónima sin token responde 401 en JSON")
    void anonymousWriteWithoutTokenRespondsUnauthorized(String method) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), RUTA_PROTEGIDA))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
    @DisplayName("Escritura anónima con token CSRF válido sigue respondiendo 401")
    void anonymousWriteWithValidTokenRespondsUnauthorized(String method) throws Exception {
        MockHttpSession session = new MockHttpSession();
        JsonNode csrf = tokenCsrf(session);

        mockMvc.perform(
            request(HttpMethod.valueOf(method), RUTA_PROTEGIDA)
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
    @DisplayName("Escritura anónima con token CSRF inválido responde 401")
    void anonymousWriteWithInvalidTokenRespondsUnauthorized(String method) throws Exception {
        MockHttpSession session = new MockHttpSession();
        JsonNode csrf = tokenCsrf(session);

        mockMvc.perform(
            request(HttpMethod.valueOf(method), RUTA_PROTEGIDA)
                .session(session)
                .header(csrf.get("headerName").asString(), "token-invalido")
        )
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
    @DisplayName("Escritura con sesión y sin token CSRF responde 403 en JSON")
    void authenticatedWriteWithoutTokenRespondsForbidden(String method) throws Exception {
        MockHttpSession session = sesionAutenticada(7);

        mockMvc.perform(request(HttpMethod.valueOf(method), RUTA_PROTEGIDA).session(session))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"))
            .andExpect(jsonPath("$.message").value("La solicitud no pudo validarse."));
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
    @DisplayName("Escritura con sesión y token CSRF inválido responde 403")
    void authenticatedWriteWithInvalidTokenRespondsForbidden(String method) throws Exception {
        MockHttpSession session = sesionAutenticada(7);
        JsonNode csrf = tokenCsrf(session);

        mockMvc.perform(
            request(HttpMethod.valueOf(method), RUTA_PROTEGIDA)
                .session(session)
                .header(csrf.get("headerName").asString(), "token-invalido")
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "POST", "PUT", "PATCH", "DELETE" })
    @DisplayName("Escritura con sesión y token CSRF válido supera la seguridad")
    void authenticatedWriteWithValidTokenPassesSecurity(String method) throws Exception {
        MockHttpSession session = sesionAutenticada(7);
        JsonNode csrf = tokenCsrf(session);

        mockMvc.perform(
            request(HttpMethod.valueOf(method), RUTA_PROTEGIDA)
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
        )
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("El token CSRF de otra sesión no es válido para esta sesión")
    void tokenFromAnotherSessionIsRejected() throws Exception {
        MockHttpSession session = sesionAutenticada(7);
        JsonNode tokenAjeno = tokenCsrf(new MockHttpSession());

        mockMvc.perform(
            post(RUTA_PROTEGIDA)
                .session(session)
                .header(tokenAjeno.get("headerName").asString(), tokenAjeno.get("token").asString())
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));
    }

    @Test
    @DisplayName("Flujo real: token, login con token y escritura con el token obtenido después")
    void loginFlowThenProtectedWriteWithFreshToken() throws Exception {
        when(userDetailsService.loadUserByUsername("ana@example.com")).thenReturn(
            new UserPrincipal(
                7,
                "Ana",
                "ana@example.com",
                "usuario",
                passwordEncoder.encode("clave-segura-1"),
                true
            )
        );
        MockHttpSession session = new MockHttpSession();
        JsonNode antesDelLogin = tokenCsrf(session);

        mockMvc.perform(
            post("/api/v1/auth/login")
                .session(session)
                .header(antesDelLogin.get("headerName").asString(), antesDelLogin.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BODY)
        )
            .andExpect(status().isOk());

        JsonNode despuesDelLogin = tokenCsrf(session);

        mockMvc.perform(post(RUTA_PROTEGIDA).session(session))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));

        mockMvc.perform(
            post(RUTA_PROTEGIDA)
                .session(session)
                .header(despuesDelLogin.get("headerName").asString(), despuesDelLogin.get("token").asString())
        )
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /csrf entrega tokens enmascarados distintos por solicitud, todos válidos en la misma sesión")
    void csrfTokensFromTheSameSessionAreAllValid() throws Exception {
        MockHttpSession session = sesionAutenticada(7);
        JsonNode primero = tokenCsrf(session);
        JsonNode segundo = tokenCsrf(session);

        assertNotEquals(primero.get("token").asString(), segundo.get("token").asString());
        for (JsonNode csrf : List.of(primero, segundo)) {
            mockMvc.perform(
                post(RUTA_PROTEGIDA)
                    .session(session)
                    .header(csrf.get("headerName").asString(), csrf.get("token").asString())
            )
                .andExpect(status().isNoContent());
        }
    }

    @Test
    @DisplayName("Rutas públicas de inicio de sesión: login sin token responde 403 y /csrf es accesible sin sesión")
    void authChainKeepsLoginProtectedAndCsrfPublic() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk());

        mockMvc.perform(
            post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_BODY)
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));
    }

    private MockHttpSession sesionAutenticada(int usuarioId) throws Exception {
        MockHttpSession session = new MockHttpSession();
        tokenCsrf(session);
        UserPrincipal principal = new UserPrincipal(
            usuarioId, "Ana", "ana@example.com", "usuario", "hash", true
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())
        );
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        );
        return session;
    }

    private JsonNode tokenCsrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
