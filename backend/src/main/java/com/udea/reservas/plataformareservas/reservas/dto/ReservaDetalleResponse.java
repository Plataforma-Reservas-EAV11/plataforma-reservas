package com.udea.reservas.plataformareservas.reservas.dto;

import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaDetalleResponse(
        Integer id,
        String servicioNombre,
        LocalDate fecha,
        LocalTime hora,
        String estado,
        String nombreUsuario,
        String emailUsuario
) {
    public static ReservaDetalleResponse fromEntity(Reserva reserva) {
        return new ReservaDetalleResponse(
                reserva.getId(),
                reserva.getServicio() != null ? reserva.getServicio().getNombre() : "Servicio general",
                reserva.getFecha(),
                reserva.getHora(),
                reserva.getEstado(),
                reserva.getUsuario().getName(),
                reserva.getUsuario().getEmail()
        );
    }
}