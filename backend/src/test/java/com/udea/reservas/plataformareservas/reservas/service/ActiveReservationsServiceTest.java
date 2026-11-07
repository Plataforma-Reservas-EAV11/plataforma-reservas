package com.udea.reservas.plataformareservas.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.reservas.dto.ActiveReservationResponse;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.repository.IReservaRepository;
import com.udea.reservas.plataformareservas.recursos.repository.RecursoRepository;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveReservationsServiceTest {

    private static final Integer USER_ID = 7;

    @Mock
    private IReservaRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IHorarioDisponibleRepository scheduleRepository;

    @Mock
    private IAgendaRepository agendaRepository;

    @Mock
    private IServicioRepository serviceRepository;

    @Mock
    private RecursoRepository resourceRepository;

    private ReservaServiceImpl reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservaServiceImpl(
            reservationRepository,
            userRepository,
            scheduleRepository,
            agendaRepository,
            serviceRepository,
            resourceRepository,
            java.time.Clock.systemUTC()
        );
    }

    @Test
    @DisplayName("Devuelve varias reservas activas ordenadas y limitadas al usuario solicitado")
    void returnsActiveReservationsInRepositoryOrder() {
        Reserva first = reservation(1, LocalDate.of(2026, 11, 6), LocalTime.of(9, 0), "activa");
        Reserva second = reservation(2, LocalDate.of(2026, 11, 7), LocalTime.of(10, 0), "ACTIVA");
        when(reservationRepository
            .findAllByUsuario_IdAndEstadoIgnoreCaseOrderByFechaAscHoraAsc(USER_ID, "activa"))
            .thenReturn(List.of(first, second));

        List<ActiveReservationResponse> result =
            reservationService.getActiveReservations(USER_ID);

        assertThat(result).containsExactly(
            new ActiveReservationResponse(1, null, LocalDate.of(2026, 11, 6),
                LocalTime.of(9, 0), null, "activa"),
            new ActiveReservationResponse(2, null, LocalDate.of(2026, 11, 7),
                LocalTime.of(10, 0), null, "ACTIVA")
        );
        verify(reservationRepository)
            .findAllByUsuario_IdAndEstadoIgnoreCaseOrderByFechaAscHoraAsc(USER_ID, "activa");
    }

    @Test
    @DisplayName("Descarta reservas canceladas, completadas y con estado nulo")
    void excludesNonActiveReservations() {
        when(reservationRepository
            .findAllByUsuario_IdAndEstadoIgnoreCaseOrderByFechaAscHoraAsc(USER_ID, "activa"))
            .thenReturn(Arrays.asList(
                reservation(1, LocalDate.of(2026, 11, 6), LocalTime.of(9, 0), "activa"),
                reservation(2, LocalDate.of(2026, 11, 6), LocalTime.of(10, 0), "cancelada"),
                reservation(3, LocalDate.of(2026, 11, 6), LocalTime.of(11, 0), "completada"),
                reservation(4, LocalDate.of(2026, 11, 6), LocalTime.of(12, 0), null),
                null
            ));

        assertThat(reservationService.getActiveReservations(USER_ID))
            .containsExactly(
                new ActiveReservationResponse(
                    1,
                    null,
                    LocalDate.of(2026, 11, 6),
                    LocalTime.of(9, 0),
                    null,
                    "activa"
                )
            );
    }

    @Test
    @DisplayName("Devuelve una lista vacía si el usuario no tiene reservas activas")
    void returnsEmptyListWhenNoActiveReservationsExist() {
        when(reservationRepository
            .findAllByUsuario_IdAndEstadoIgnoreCaseOrderByFechaAscHoraAsc(USER_ID, "activa"))
            .thenReturn(List.of());

        assertThat(reservationService.getActiveReservations(USER_ID)).isEmpty();
    }

    private static Reserva reservation(
        Integer id,
        LocalDate date,
        LocalTime time,
        String status
    ) {
        return Reserva.builder()
            .id(id)
            .fecha(date)
            .hora(time)
            .estado(status)
            .build();
    }
}
