package com.udea.reservas.plataformareservas.agendas.dto;

import java.time.LocalTime;

public record HorarioResponse(
        Integer id,
        Integer agendaId,
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer duracionSlotMin
) {}