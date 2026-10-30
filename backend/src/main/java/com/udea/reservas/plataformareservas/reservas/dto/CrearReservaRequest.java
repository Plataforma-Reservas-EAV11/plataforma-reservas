package com.udea.reservas.plataformareservas.reservas.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Datos de la reserva. El usuario no viaja aquí (DEF-01): se toma de la sesión
 * autenticada, de modo que nadie puede reservar a nombre de otra persona.
 */
public record CrearReservaRequest(
        @NotNull Integer horarioId,
        @NotNull LocalDate fecha,
        @NotNull LocalTime hora
) {
}
