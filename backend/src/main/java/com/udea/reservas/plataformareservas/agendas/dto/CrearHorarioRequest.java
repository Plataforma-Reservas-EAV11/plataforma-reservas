package com.udea.reservas.plataformareservas.agendas.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CrearHorarioRequest(
        @NotNull(message = "El día de la semana es obligatorio")
        Integer diaSemana,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @NotNull(message = "La duración del slot es obligatoria")
        Integer duracionSlotMin
) {}