package com.udea.reservas.plataformareservas.agendas.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalTime;

public record CrearHorarioRequest(
        @NotNull(message = "El día de la semana es obligatorio")
        @Min(value = 0, message = "El día de la semana debe estar entre 0 (lunes) y 6 (domingo)")
        @Max(value = 6, message = "El día de la semana debe estar entre 0 (lunes) y 6 (domingo)")
        Integer diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @NotNull(message = "La duración del slot es obligatoria")
        @Positive(message = "La duración del slot debe ser mayor que cero")
        Integer duracionSlotMin
) {}
