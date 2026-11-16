package com.udea.reservas.plataformareservas.reservas.dto;

import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaHistorialResponse(
        Integer id,
        String servicioNombre,
        LocalDate fecha,
        LocalTime hora,
        String estado
) {
    public static ReservaHistorialResponse fromEntity(Reserva reserva) {
        return new ReservaHistorialResponse(
                reserva.getId(),
                reserva.getServicio() != null ? reserva.getServicio().getNombre() : "Servicio general",
                reserva.getFecha(),
                reserva.getHora(),
                reserva.getEstado()
        );
    }
}