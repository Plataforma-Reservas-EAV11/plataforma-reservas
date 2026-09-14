package com.udea.reservas.plataformareservas.agendas.dto;

import java.time.LocalTime;

public record CrearHorarioRequest(
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer duracionSlotMin
) {}