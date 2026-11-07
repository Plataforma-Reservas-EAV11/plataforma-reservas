package com.udea.reservas.plataformareservas.reservas.service;

import com.udea.reservas.plataformareservas.reservas.dto.ActiveReservationResponse;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaService {

    List<ActiveReservationResponse> getActiveReservations(Integer userId);

    Reserva crearReserva(Integer usuarioId, Integer horarioId, LocalDate fecha, LocalTime hora);

    Reserva cancelarReserva(Integer reservaId, Integer usuarioId);
}
