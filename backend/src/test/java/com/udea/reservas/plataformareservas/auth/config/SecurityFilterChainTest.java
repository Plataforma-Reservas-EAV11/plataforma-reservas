package com.udea.reservas.plataformareservas.auth.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.auth.controller.UserController;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.auth.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@Import({ SecurityConfiguration.class, PasswordEncoderConfiguration.class })
@EnableWebSecurity
class SecurityFilterChainTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Rechaza el inicio de sesión sin token CSRF con un 403 en JSON")
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        // Arrange
        String body = LOGIN_BODY;

        // Act
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            // Assert
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"))
            .andExpect(jsonPath("$.message").value("La solicitud no pudo validarse."))
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    @DisplayName("Inicia sesión con el token CSRF entregado por /csrf en la misma sesión")
    void loginWithCsrfTokenFromSameSessionSucceeds() throws Exception {
        // Arrange
        when(userDetailsService.loadUserByUsername("ana@example.com")).thenReturn(
            new UserPrincipal(
                7,
                "Ana",
                "ana@example.com",
                "usuario",
                passwordEncoder.encode("clave-segura-1")
            )
        );
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession();
        JsonNode csrf = objectMapper.readTree(csrfResult.getResponse().getContentAsString());

        // Act
        mockMvc.perform(
            post("/api/v1/auth/login")
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BODY)
        )
            // Assert
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(7))
            .andExpect(jsonPath("$.data.email").value("ana@example.com"))
            .andExpect(jsonPath("$.data.rol").value("usuario"));
    }

    @Test
    @DisplayName("Responde 401 genérico cuando la contraseña no coincide")
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        // Arrange
        when(userDetailsService.loadUserByUsername("ana@example.com")).thenReturn(
            new UserPrincipal(
                7,
                "Ana",
                "ana@example.com",
                "usuario",
                passwordEncoder.encode("otra-clave-distinta")
            )
        );
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession();
        JsonNode csrf = objectMapper.readTree(csrfResult.getResponse().getContentAsString());

        // Act
        mockMvc.perform(
            post("/api/v1/auth/login")
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(LOGIN_BODY)
        )
            // Assert
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
    }
}
