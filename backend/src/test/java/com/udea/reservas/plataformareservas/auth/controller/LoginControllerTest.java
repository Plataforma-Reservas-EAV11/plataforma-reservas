package com.udea.reservas.plataformareservas.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.auth.dto.CsrfTokenResponse;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.auth.service.UserService;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private CsrfToken csrfToken;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void loginPersistsAuthenticatedContextAndReturnsOnlyPublicUserData()
        throws Exception {
        UserPrincipal principal = new UserPrincipal(
            7,
            "Ana",
            "ana@example.com",
            "$2a$hashed-value"
        );
        Authentication authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of()
            );
        when(authenticationManager.authenticate(any(Authentication.class)))
            .thenReturn(authentication);
        MockHttpSession session = new MockHttpSession();
        String oldSessionId = session.getId();

        mockMvc.perform(
            post("/api/v1/auth/login")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoginPayload("ANA@EXAMPLE.COM", "secret")
                    )
                )
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(7))
            .andExpect(jsonPath("$.data.name").value("Ana"))
            .andExpect(jsonPath("$.data.email").value("ana@example.com"))
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.data.password").doesNotExist());

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(securityContextRepository).saveContext(
            any(),
            any(),
            any()
        );
        org.junit.jupiter.api.Assertions.assertNotEquals(oldSessionId, session.getId());
    }

    @Test
    void loginReturnsSameGenericUnauthorizedResponseForInvalidCredentials()
        throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
            .thenThrow(new BadCredentialsException("Internal detail"));

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoginPayload("missing@example.com", "secret")
                    )
                )
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
            .andExpect(jsonPath("$.message").value(
                "El correo electrónico o la contraseña no son correctos."
            ));

        verify(securityContextRepository, never()).saveContext(
            any(),
            any(),
            any()
        );
    }

    @Test
    void loginRejectsInvalidFieldsBeforeAuthentication() throws Exception {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoginPayload("not-an-email", "")
                    )
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.email").exists())
            .andExpect(jsonPath("$.errors.password").exists());

        verify(authenticationManager, never()).authenticate(
            any(Authentication.class)
        );
        verify(securityContextRepository, never()).saveContext(
            any(),
            any(),
            any()
        );
    }

    @Test
    void csrfReturnsTokenMetadata() throws Exception {
        // Arrange
        when(csrfToken.getHeaderName()).thenReturn("X-XSRF-TOKEN");
        when(csrfToken.getParameterName()).thenReturn("_csrf");
        when(csrfToken.getToken()).thenReturn("token-value");

        // Act
        CsrfTokenResponse result = userController.csrfToken(csrfToken);

        // Assert
        assertEquals("X-XSRF-TOKEN", result.headerName());
        assertEquals("_csrf", result.parameterName());
        assertEquals("token-value", result.token());
    }

    private record LoginPayload(String email, String password) {}
}
