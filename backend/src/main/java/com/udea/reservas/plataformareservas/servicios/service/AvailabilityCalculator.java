package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.servicios.dto.AvailableSlotResponse;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class AvailabilityCalculator {

    private static final String ACTIVE_RESERVATION = "activa";

    private AvailabilityCalculator() {}

    static List<Recurso> activeResourcesFor(Servicio service, Integer providerId) {
        List<Recurso> compatibleResources = service.getRecursosCompatibles();
        if (compatibleResources == null) {
            return List.of();
        }
        return compatibleResources.stream()
            .filter(Objects::nonNull)
            .filter(resource -> resource.getId() != null)
            .filter(Recurso::isActivo)
            .filter(resource -> resource.getProveedor() != null)
            .filter(resource -> providerId.equals(resource.getProveedor().getId()))
            .toList();
    }

    static Map<LocalDate, Map<Integer, List<Reserva>>> indexActiveReservations(
        List<Reserva> reservations
    ) {
        return reservations.stream()
            .filter(reservation -> ACTIVE_RESERVATION.equalsIgnoreCase(reservation.getEstado()))
            .filter(reservation -> reservation.getRecurso() != null)
            .filter(reservation -> reservation.getRecurso().getId() != null)
            .filter(reservation -> reservation.getHora() != null)
            .filter(reservation -> reservation.getHoraFin() != null)
            .collect(Collectors.groupingBy(
                Reserva::getFecha,
                Collectors.groupingBy(reservation -> reservation.getRecurso().getId())
            ));
    }

    static Comparator<AvailableSlotResponse> slotOrder() {
        return Comparator.comparing(AvailableSlotResponse::fecha)
            .thenComparing(AvailableSlotResponse::hora)
            .thenComparing(AvailableSlotResponse::horarioId);
    }

    static List<AvailableSlotResponse> slotsFor(
        HorarioDisponible schedule,
        Servicio service,
        List<Recurso> activeResources,
        Map<LocalDate, Map<Integer, List<Reserva>>> reservationsByDate,
        AvailabilityWindow window
    ) {
        CatalogDataValidator.validateSchedule(schedule);
        if (!belongsToService(schedule, service) || !fitsServiceDuration(schedule, service)) {
            return List.of();
        }

        return window.startDate()
            .datesUntil(window.endDate().plusDays(1))
            .filter(date -> matchesWeekday(date, schedule))
            .flatMap(date ->
                slotsForDate(
                    date,
                    schedule,
                    service,
                    activeResources,
                    reservationsByDate,
                    window.now()
                ).stream()
            )
            .toList();
    }

    private static boolean belongsToService(HorarioDisponible schedule, Servicio service) {
        Optional<Agenda> agenda = Optional.ofNullable(schedule.getAgenda())
            .filter(Agenda::isActiva);
        boolean sameService = agenda
            .map(Agenda::getServicio)
            .map(Servicio::getId)
            .filter(id -> Objects.equals(id, service.getId()))
            .isPresent();
        boolean sameProvider = agenda
            .map(Agenda::getProveedor)
            .map(Proveedor::getId)
            .filter(id -> Objects.equals(id, service.getProveedor().getId()))
            .isPresent();
        return sameService && sameProvider;
    }

    private static boolean fitsServiceDuration(HorarioDisponible schedule, Servicio service) {
        long scheduleDurationMinutes = Duration.between(
            schedule.getHoraInicio(),
            schedule.getHoraFin()
        ).toMinutes();
        return Math.max(
            schedule.getDuracionSlotMin(),
            service.getDuracionMinutos()
        ) <= scheduleDurationMinutes;
    }

    private static boolean matchesWeekday(LocalDate date, HorarioDisponible schedule) {
        return date.getDayOfWeek().getValue() - 1 == schedule.getDiaSemana();
    }

    private static List<AvailableSlotResponse> slotsForDate(
        LocalDate date,
        HorarioDisponible schedule,
        Servicio service,
        List<Recurso> activeResources,
        Map<LocalDate, Map<Integer, List<Reserva>>> reservationsByDate,
        LocalDateTime now
    ) {
        int intervalMinutes = schedule.getDuracionSlotMin();
        int serviceDurationMinutes = service.getDuracionMinutos();
        return Stream.iterate(
            schedule.getHoraInicio(),
            time -> time.plusMinutes(intervalMinutes)
        )
            .takeWhile(time ->
                !time.plusMinutes(intervalMinutes).isAfter(schedule.getHoraFin())
            )
            .filter(time ->
                isBookable(
                    date,
                    time,
                    serviceDurationMinutes,
                    schedule,
                    activeResources,
                    reservationsByDate,
                    now
                )
            )
            .map(time -> new AvailableSlotResponse(schedule.getId(), date, time))
            .toList();
    }

    private static boolean isBookable(
        LocalDate date,
        LocalTime startTime,
        int serviceDurationMinutes,
        HorarioDisponible schedule,
        List<Recurso> activeResources,
        Map<LocalDate, Map<Integer, List<Reserva>>> reservationsByDate,
        LocalDateTime now
    ) {
        LocalTime endTime = startTime.plusMinutes(serviceDurationMinutes);
        return Stream.of(
            !endTime.isAfter(schedule.getHoraFin()),
            LocalDateTime.of(date, startTime).isAfter(now),
            hasAvailableResource(reservationsByDate, activeResources, date, startTime, endTime)
        ).allMatch(Boolean::booleanValue);
    }

    private static boolean hasAvailableResource(
        Map<LocalDate, Map<Integer, List<Reserva>>> reservationsByDate,
        List<Recurso> resources,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    ) {
        Map<Integer, List<Reserva>> reservationsByResource = reservationsByDate.getOrDefault(
            date,
            Map.of()
        );
        return resources.stream()
            .anyMatch(resource ->
                reservationsByResource.getOrDefault(resource.getId(), List.of())
                    .stream()
                    .noneMatch(reservation -> overlaps(reservation, startTime, endTime))
            );
    }

    private static boolean overlaps(Reserva reservation, LocalTime startTime, LocalTime endTime) {
        return startTime.isBefore(reservation.getHoraFin())
            && endTime.isAfter(reservation.getHora());
    }
}
