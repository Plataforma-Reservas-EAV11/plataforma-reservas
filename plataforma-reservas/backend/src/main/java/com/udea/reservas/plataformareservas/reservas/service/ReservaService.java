package com.udea.reservas.plataformareservas.reservas.service;

import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;

public interface ReservaService {

    Reserva crearReserva(Integer usuarioId, Integer horarioId, LocalDate fecha, LocalTime hora);

    Reserva cancelarReserva(Integer reservaId, Integer usuarioId);
}
