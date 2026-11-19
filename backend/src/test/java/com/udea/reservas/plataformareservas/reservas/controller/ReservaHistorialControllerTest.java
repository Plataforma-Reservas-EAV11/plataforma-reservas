package com.udea.reservas.plataformareservas.reservas.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.TestAuthentication;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import com.udea.reservas.plataformareservas.reservas.dto.ReservaDetalleResponse;
import com.udea.reservas.plataformareservas.reservas.dto.ReservaHistorialResponse;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReservaHistorialControllerTest {

    private static final int USUARIO_SESION = 1;

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private ReservaController reservaController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        TestAuthentication.authenticateAs(
                TestAuthentication.principal(USUARIO_SESION)
        );
        this.mockMvc = MockMvcBuilders.standaloneSetup(reservaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(TestAuthentication.argumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Obtener historial de reservas del usuario autenticado retorna 200 OK")
    void obtenerHistorialExitoso() throws Exception {
        when(reservaService.obtenerHistorialUsuario(anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/reservas/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.message").value("Aún no tienes reservas registradas."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(reservaService).obtenerHistorialUsuario(USUARIO_SESION);
    }

    @Test
    @DisplayName("El historial conserva el arreglo y usa el usuario de la sesión")
    void historialConReservasIgnoraIdentidadEnLaPeticion() throws Exception {
        int usuarioAjeno = 2;
        var primera = new ReservaHistorialResponse(
                101, "Corte", LocalDate.of(2026, 11, 20), LocalTime.of(10, 0), "activa"
        );
        var segunda = new ReservaHistorialResponse(
                100, "Manicure", LocalDate.of(2026, 11, 19), LocalTime.of(9, 0), "cancelada"
        );
        when(reservaService.obtenerHistorialUsuario(USUARIO_SESION))
                .thenReturn(List.of(primera, segunda));

        mockMvc.perform(get("/api/reservas/historial")
                        .param("usuarioId", String.valueOf(usuarioAjeno))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", aMapWithSize(5)))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].servicioNombre").value("Corte"))
                .andExpect(jsonPath("$[0].fecha").value("2026-11-20"))
                .andExpect(jsonPath("$[0].hora").value("10:00:00"))
                .andExpect(jsonPath("$[0].estado").value("activa"))
                .andExpect(jsonPath("$[1]", aMapWithSize(5)))
                .andExpect(jsonPath("$[1].id").value(100))
                .andExpect(jsonPath("$[1].servicioNombre").value("Manicure"))
                .andExpect(jsonPath("$[1].fecha").value("2026-11-19"))
                .andExpect(jsonPath("$[1].hora").value("09:00:00"))
                .andExpect(jsonPath("$[1].estado").value("cancelada"));

        verify(reservaService).obtenerHistorialUsuario(USUARIO_SESION);
        verify(reservaService, never()).obtenerHistorialUsuario(usuarioAjeno);
    }

    @Test
    @DisplayName("Obtener detalle de una reserva por ID retorna 200 OK")
    void obtenerDetalleExitoso() throws Exception {
        int reservaId = 1;
        ReservaDetalleResponse responseMock = mock(ReservaDetalleResponse.class);
        when(reservaService.obtenerDetalleReserva(eq(reservaId), anyInt())).thenReturn(responseMock);

        mockMvc.perform(get("/api/reservas/{id}/detalle", reservaId))
                .andExpect(status().isOk());
    }
}