package com.udea.reservas.plataformareservas.reservas.dto;

import com.udea.reservas.plataformareservas.reservas.model.Reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservaResponse(
        Integer id,
        Integer usuarioId,
        Integer horarioId,
        Integer servicioId,
        Integer recursoId,
        LocalDate fecha,
        LocalTime hora,
        LocalTime horaFin,
        String estado,
        LocalDateTime createdAt
) {
    public static ReservaResponse fromEntity(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getUsuario().getId(),
                reserva.getHorario().getId(),
                reserva.getServicio() == null ? null : reserva.getServicio().getId(),
                reserva.getRecurso() == null ? null : reserva.getRecurso().getId(),
                reserva.getFecha(),
                reserva.getHora(),
                reserva.getHoraFin(),
                reserva.getEstado(),
                reserva.getCreatedAt()
        );
    }
}