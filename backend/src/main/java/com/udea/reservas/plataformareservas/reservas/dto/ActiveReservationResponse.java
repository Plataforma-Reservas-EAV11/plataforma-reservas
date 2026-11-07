package com.udea.reservas.plataformareservas.reservas.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record ActiveReservationResponse(
    Integer id,
    String serviceName,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String status
) {}
