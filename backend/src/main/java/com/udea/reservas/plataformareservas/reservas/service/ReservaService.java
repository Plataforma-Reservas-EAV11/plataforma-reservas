package com.udea.reservas.plataformareservas.reservas.service;

import com.udea.reservas.plataformareservas.reservas.dto.ActiveReservationResponse;
import com.udea.reservas.plataformareservas.reservas.dto.ReservaDetalleResponse;
import com.udea.reservas.plataformareservas.reservas.dto.ReservaHistorialResponse;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaService {

    List<ActiveReservationResponse> getActiveReservations(Integer usuarioId);

    Reserva crearReserva(Integer usuarioId, Integer servicioId, LocalDate fecha, LocalTime hora);

    Reserva cancelarReserva(Integer reservaId, Integer usuarioId);

    List<ReservaHistorialResponse> obtenerHistorialUsuario(Integer usuarioId);

    ReservaDetalleResponse obtenerDetalleReserva(Integer reservaId, Integer usuarioId);
}
