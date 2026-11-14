package com.udea.reservas.plataformareservas.servicios.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

record AvailabilityWindow(LocalDate startDate, LocalDate endDate, LocalDateTime now) {

    static AvailabilityWindow startingAt(LocalDateTime now, int days) {
        LocalDate startDate = now.toLocalDate();
        return new AvailabilityWindow(startDate, startDate.plusDays(days - 1L), now);
    }
}
