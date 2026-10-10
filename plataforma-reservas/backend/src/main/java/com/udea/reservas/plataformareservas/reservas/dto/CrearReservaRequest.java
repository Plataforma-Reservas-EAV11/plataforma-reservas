package com.udea.reservas.plataformareservas.reservas.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CrearReservaRequest(
        @NotNull Integer usuarioId,
        @NotNull Integer horarioId,
        @NotNull LocalDate fecha,
        @NotNull LocalTime hora
) {
}