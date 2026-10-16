package com.udea.reservas.plataformareservas.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationRequest;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationResponse;
import com.udea.reservas.plataformareservas.proveedores.service.ProviderRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ProviderControllerTest {

    @Mock
    private ProviderRegistrationService registrationService;

    @InjectMocks
    private ProviderController providerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(providerController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Registra un proveedor con respuesta 201 sin exponer la contraseña")
    void registerValidRequestReturnsCreatedResponse() throws Exception {
        // Arrange
        ProviderRegistrationRequest request = validRequest();
        ProviderRegistrationResponse response = new ProviderRegistrationResponse(
            47,
            31,
            "Ana Pérez",
            "ana@example.com",
            "MiNegocio",
            "3001234567"
        );
        when(registrationService.register(any(ProviderRegistrationRequest.class)))
            .thenReturn(response);

        // Act and Assert
        mockMvc.perform(
            post("/api/v1/providers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.message")
                    .value("Registro de proveedor exitoso. Ya puedes iniciar sesión.")
            )
            .andExpect(jsonPath("$.data.providerId").value(47))
            .andExpect(jsonPath("$.data.userId").value(31))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(registrationService).register(request);
    }

    @Test
    @DisplayName("Devuelve detalles de validación cuando faltan campos obligatorios")
    void registerEmptyBodyReturnsValidationError() throws Exception {
        // Act and Assert
        mockMvc.perform(
            post("/api/v1/providers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.details[0]").isNotEmpty())
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verify(registrationService, never()).register(any(ProviderRegistrationRequest.class));
    }

    @Test
    @DisplayName("Devuelve conflicto cuando el correo ya está registrado")
    void registerDuplicateEmailReturnsConflict() throws Exception {
        // Arrange
        when(registrationService.register(any(ProviderRegistrationRequest.class)))
            .thenThrow(
                new BusinessRuleException(
                    "EMAIL_ALREADY_REGISTERED",
                    "El correo ya está registrado.",
                    HttpStatus.CONFLICT.value()
                )
            );

        // Act and Assert
        mockMvc.perform(
            post("/api/v1/providers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_REGISTERED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Devuelve error de negocio cuando las contraseñas no coinciden")
    void registerPasswordMismatchReturnsBadRequest() throws Exception {
        // Arrange
        when(registrationService.register(any(ProviderRegistrationRequest.class)))
            .thenThrow(
                new BusinessRuleException(
                    "PASSWORD_MISMATCH",
                    "Las contraseñas no coinciden.",
                    HttpStatus.BAD_REQUEST.value()
                )
            );

        // Act and Assert
        mockMvc.perform(
            post("/api/v1/providers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest()))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("PASSWORD_MISMATCH"))
            .andExpect(jsonPath("$.message").value("Las contraseñas no coinciden."))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Devuelve error controlado cuando el cuerpo no es JSON válido")
    void registerMalformedJsonReturnsBadRequest() throws Exception {
        // Act and Assert
        mockMvc.perform(
            post("/api/v1/providers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"))
            .andExpect(
                jsonPath("$.message")
                    .value("El cuerpo de la solicitud no es un JSON válido.")
            )
            .andExpect(jsonPath("$.traceId").isNotEmpty());

        verify(registrationService, never()).register(any(ProviderRegistrationRequest.class));
    }

    @Test
    @DisplayName("Rechaza contraseñas de más de 72 bytes aunque tengan menos de 72 caracteres")
    void registerPasswordOverSeventyTwoBytesReturnsValidationError() throws Exception {
        // Arrange
        String password = "ñ".repeat(40);
        ProviderRegistrationRequest request = new ProviderRegistrationRequest(
            "Ana Pérez",
            "MiNegocio",
            "ana@example.com",
            password,
            password,
            "3001234567"
        );

        // Act and Assert
        mockMvc.perform(
            post("/api/v1/providers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[0]").value("password es demasiado larga"));

        verify(registrationService, never()).register(any(ProviderRegistrationRequest.class));
    }

    private ProviderRegistrationRequest validRequest() {
        return new ProviderRegistrationRequest(
            "Ana Pérez",
            "MiNegocio",
            "ana@example.com",
            "clave-segura",
            "clave-segura",
            "3001234567"
        );
    }
}
