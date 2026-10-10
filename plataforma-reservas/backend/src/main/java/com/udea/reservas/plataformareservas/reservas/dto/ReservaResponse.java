package com.udea.reservas.plataformareservas.reservas.dto;

import com.udea.reservas.plataformareservas.reservas.model.Reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservaResponse(
        Integer id,
        Integer usuarioId,
        Integer horarioId,
        LocalDate fecha,
        LocalTime hora,
        String estado,
        LocalDateTime createdAt
) {
    public static ReservaResponse fromEntity(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getUsuario().getId(),
                reserva.getHorario().getId(),
                reserva.getFecha(),
                reserva.getHora(),
                reserva.getEstado(),
                reserva.getCreatedAt()
        );
    }
}