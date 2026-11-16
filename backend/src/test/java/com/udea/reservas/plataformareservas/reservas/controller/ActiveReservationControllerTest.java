package com.udea.reservas.plataformareservas.reservas.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.udea.reservas.plataformareservas.TestAuthentication;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import com.udea.reservas.plataformareservas.reservas.dto.ActiveReservationResponse;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
class ActiveReservationControllerTest {

    private static final int USER_ID = 7;

    @Mock
    private ReservaService reservationService;

    @InjectMocks
    private ActiveReservationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TestAuthentication.authenticateAs(TestAuthentication.principal(USER_ID));
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(TestAuthentication.argumentResolver())
            .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Responde con los campos del contrato y usa el usuario autenticado")
    void returnsActiveReservationsFromAuthenticatedUser() throws Exception {
        when(reservationService.getActiveReservations(USER_ID)).thenReturn(List.of(
            new ActiveReservationResponse(
                15,
                null,
                LocalDate.of(2026, 11, 10),
                LocalTime.of(9, 0),
                null,
                "activa"
            )
        ));

        mockMvc.perform(get("/api/v1/reservations/me/active").queryParam("userId", "99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(15))
            .andExpect(jsonPath("$[0].serviceName").value(nullValue()))
            .andExpect(jsonPath("$[0].date").value("2026-11-10"))
            .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
            .andExpect(jsonPath("$[0].endTime").value(nullValue()))
            .andExpect(jsonPath("$[0].status").value("activa"));

        verify(reservationService).getActiveReservations(USER_ID);
    }

    @Test
    @DisplayName("Devuelve arreglo vacío si no hay reservas activas")
    void returnsEmptyArrayWhenUserHasNoActiveReservations() throws Exception {
        when(reservationService.getActiveReservations(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reservations/me/active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        verify(reservationService).getActiveReservations(USER_ID);
    }
}
