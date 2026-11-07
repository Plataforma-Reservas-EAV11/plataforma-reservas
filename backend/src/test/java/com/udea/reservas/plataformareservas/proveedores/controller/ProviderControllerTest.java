package com.udea.reservas.plataformareservas.proveedores.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import com.udea.reservas.plataformareservas.common.exception.ProviderApiExceptionHandler;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationRequest;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationResponse;
import com.udea.reservas.plataformareservas.proveedores.service.ProviderRegistrationService;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderPageResponse;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderSummaryResponse;
import com.udea.reservas.plataformareservas.servicios.service.ServiceCatalogService;
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
import java.util.List;

@ExtendWith(MockitoExtension.class)
class ProviderControllerTest {

    @Mock
    private ProviderRegistrationService registrationService;

    @Mock
    private ServiceCatalogService serviceCatalogService;

    @InjectMocks
    private ProviderController providerController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(providerController)
            .setControllerAdvice(new ProviderApiExceptionHandler(new GlobalExceptionHandler()), new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Lista proveedores con paginación y sólo los campos públicos")
    void listProvidersReturnsPaginatedPublicFields() throws Exception {
        ProviderPageResponse page = new ProviderPageResponse(
            List.of(new ProviderSummaryResponse(47, "MiNegocio")),
            1,
            5,
            6,
            2,
            false,
            true,
            false
        );
        when(serviceCatalogService.getProviders(1, 5)).thenReturn(page);

        mockMvc.perform(get("/api/v1/providers").queryParam("page", "1").queryParam("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Proveedores consultados correctamente."))
            .andExpect(jsonPath("$.data.content[0].id").value(47))
            .andExpect(jsonPath("$.data.content[0].nombre").value("MiNegocio"))
            .andExpect(jsonPath("$.data.content[0].telefono").doesNotExist())
            .andExpect(jsonPath("$.data.page").value(1))
            .andExpect(jsonPath("$.data.size").value(5))
            .andExpect(jsonPath("$.data.totalElements").value(6))
            .andExpect(jsonPath("$.data.totalPages").value(2))
            .andExpect(jsonPath("$.data.first").value(false))
            .andExpect(jsonPath("$.data.last").value(true))
            .andExpect(jsonPath("$.data.empty").value(false));

        verify(serviceCatalogService).getProviders(1, 5);
    }

    @Test
    @DisplayName("Devuelve resultado vacío cuando no hay proveedores")
    void listProvidersReturnsEmptyPage() throws Exception {
        when(serviceCatalogService.getProviders(0, 20)).thenReturn(
            new ProviderPageResponse(List.of(), 0, 20, 0, 0, true, true, true)
        );

        mockMvc.perform(get("/api/v1/providers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isEmpty())
            .andExpect(jsonPath("$.data.empty").value(true));
    }

    @Test
    @DisplayName("Devuelve error controlado para parámetros de paginación inválidos")
    void listProvidersReturnsBadRequestForInvalidPagination() throws Exception {
        when(serviceCatalogService.getProviders(0, 101)).thenThrow(
            new BusinessRuleException(
                "INVALID_PAGINATION",
                "La paginación debe tener una página no negativa y un tamaño entre 1 y 100.",
                HttpStatus.BAD_REQUEST.value()
            )
        );

        mockMvc.perform(get("/api/v1/providers").queryParam("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_PAGINATION"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Devuelve error de tipo inválido para parámetros no enteros")
    void listProvidersReturnsBadRequestForNonNumericPagination() throws Exception {
        mockMvc.perform(get("/api/v1/providers").queryParam("page", "first"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));

        verify(serviceCatalogService, never()).getProviders(anyInt(), anyInt());
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
        // Arrange
        String emptyBody = "{}";

        // Act and Assert
        mockMvc.perform(
            post("/api/v1/providers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyBody)
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
        // Arrange
        String malformedJson = "{invalid-json";

        // Act and Assert
        mockMvc.perform(
            post("/api/v1/providers/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson)
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
