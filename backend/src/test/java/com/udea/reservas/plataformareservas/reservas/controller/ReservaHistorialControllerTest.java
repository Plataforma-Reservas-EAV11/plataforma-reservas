package com.udea.reservas.plataformareservas.reservas.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.TestAuthentication;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import com.udea.reservas.plataformareservas.reservas.dto.ReservaDetalleResponse;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Obtener detalle de una reserva por ID retorna 200 OK")
    void obtenerDetalleExitoso() throws Exception {
        int reservaId = 1;
        ReservaDetalleResponse responseMock = org.mockito.Mockito.mock(ReservaDetalleResponse.class);
        when(reservaService.obtenerDetalleReserva(eq(reservaId), anyInt())).thenReturn(responseMock);

        mockMvc.perform(get("/api/reservas/{id}/detalle", reservaId))
                .andExpect(status().isOk());
    }
}