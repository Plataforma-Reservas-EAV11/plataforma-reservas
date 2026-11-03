package com.udea.reservas.plataformareservas.servicios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.TestAuthentication;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;
import com.udea.reservas.plataformareservas.servicios.service.ServicioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ServicioControllerTest {

    private static final String RUTA = "/api/v1/providers/me/services";

    @Mock
    private ServicioService servicioService;

    @InjectMocks
    private ServicioController servicioController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(servicioController)
            .setCustomArgumentResolvers(TestAuthentication.argumentResolver())
            .build();
        objectMapper = new ObjectMapper();
        TestAuthentication.authenticateAs(
            new UserPrincipal(7, "Ana", "ana@example.com", "proveedor", "hash", true)
        );
    }

    @Test
    @DisplayName("Crea un servicio con respuesta 201 y usa al usuario autenticado")
    void crearServicioValidoRetornaCreated() throws Exception {
        CrearServicioRequest request = new CrearServicioRequest(
            "Corte",
            "Corte de cabello",
            30
        );
        when(servicioService.crearServicio(7, request)).thenReturn(
            new ServicioResponse(41, "Corte", "Corte de cabello", 30)
        );

        mockMvc.perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(41))
            .andExpect(jsonPath("$.data.nombre").value("Corte"))
            .andExpect(jsonPath("$.data.duracion").value(30))
            .andExpect(jsonPath("$.data.proveedorId").doesNotExist());

        verify(servicioService).crearServicio(7, request);
    }

    @Test
    @DisplayName("Rechaza campos ausentes o blancos sin invocar el servicio")
    void crearServicioInvalidoRetornaErroresDeValidacion() throws Exception {
        mockMvc.perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"   \",\"descripcion\":\"\",\"duracion\":null}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details").isArray())
            .andExpect(jsonPath("$.details").isNotEmpty());

        mockMvc.perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(servicioService, never()).crearServicio(any(Integer.class), any());
    }

    @Test
    @DisplayName("Rechaza una duración cero o negativa")
    void crearServicioDuracionNoPositivaEsInvalida() throws Exception {
        for (int duracion : new int[] { 0, -1 }) {
            mockMvc.perform(
                post(RUTA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"nombre\":\"Corte\",\"descripcion\":\"Descripción\",\"duracion\":" +
                        duracion +
                        "}"
                    )
            ).andExpect(status().isBadRequest());
        }

        verify(servicioService, never()).crearServicio(any(Integer.class), any());
    }

    @Test
    @DisplayName("Rechaza duración decimal y no persiste el servicio")
    void crearServicioDuracionDecimalEsInvalida() throws Exception {
        mockMvc.perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Corte\",\"descripcion\":\"Descripción\",\"duracion\":2.5}"
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[0]").value("duracion debe ser un número entero."));

        verify(servicioService, never()).crearServicio(any(Integer.class), any());
    }

    @Test
    @DisplayName("Acepta los límites de longitud y una duración positiva sin máximo de negocio")
    void crearServicioAceptaLimitesDeLongitudYDuracionEnteraGrande() throws Exception {
        String nombre = "n".repeat(150);
        String descripcion = "d".repeat(500);
        CrearServicioRequest request = new CrearServicioRequest(
            nombre,
            descripcion,
            Integer.MAX_VALUE
        );
        when(servicioService.crearServicio(7, request)).thenReturn(
            new ServicioResponse(41, nombre, descripcion, Integer.MAX_VALUE)
        );

        mockMvc.perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.nombre").value(nombre))
            .andExpect(jsonPath("$.data.descripcion").value(descripcion))
            .andExpect(jsonPath("$.data.duracion").value(Integer.MAX_VALUE));

        verify(servicioService).crearServicio(7, request);
    }

    @Test
    @DisplayName("Devuelve el error de negocio con el código HTTP y código acordados")
    void crearServicioPerfilNoEncontradoResponde404() throws Exception {
        doThrow(
            new BusinessRuleException(
                "PROVIDER_PROFILE_NOT_FOUND",
                "No existe un perfil de proveedor para el usuario autenticado.",
                HttpStatus.NOT_FOUND.value()
            )
        ).when(servicioService).crearServicio(any(Integer.class), any());

        mockMvc.perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Corte\",\"descripcion\":\"Descripción\",\"duracion\":30}")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("PROVIDER_PROFILE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Devuelve un error controlado para JSON malformado")
    void crearServicioJsonMalformadoResponde400() throws Exception {
        mockMvc.perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));

        verify(servicioService, never()).crearServicio(any(Integer.class), any());
    }

    @Test
    @DisplayName("Rechaza nombre y descripción que superan sus límites")
    void crearServicioCamposDemasiadoLargosSonInvalidos() throws Exception {
        String nombre = "n".repeat(151);
        String descripcion = "d".repeat(501);
        mockMvc.perform(
            post(RUTA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new CrearServicioRequest(nombre, descripcion, 30)
                    )
                )
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(servicioService, never()).crearServicio(any(Integer.class), any());
    }
}
