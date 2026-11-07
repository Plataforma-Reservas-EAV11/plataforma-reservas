package com.udea.reservas.plataformareservas.servicios.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailableSlotResponse(
    Integer horarioId,
    LocalDate fecha,
    LocalTime hora
) {}
