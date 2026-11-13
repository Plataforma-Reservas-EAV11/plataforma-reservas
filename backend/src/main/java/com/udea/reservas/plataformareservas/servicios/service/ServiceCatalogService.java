package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.repository.IReservaRepository;
import com.udea.reservas.plataformareservas.servicios.dto.AvailableSlotResponse;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderPageResponse;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderServiceResponse;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderSummaryResponse;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private static final int PAGE_SIZE_MAX = 100;
    private static final int AVAILABILITY_DAYS = 30;
    private static final String ACTIVE_RESERVATION = "activa";

    private final IProveedorRepository providerRepository;
    private final IServicioRepository serviceRepository;
    private final IHorarioDisponibleRepository scheduleRepository;
    private final IReservaRepository reservationRepository;
    private final IAgendaRepository agendaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ProviderPageResponse getProviders(int page, int size) {
        if (page < 0 || size < 1 || size > PAGE_SIZE_MAX) {
            throw new BusinessRuleException(
                "INVALID_PAGINATION",
                "La paginación debe tener una página no negativa y un tamaño entre 1 y 100.",
                HttpStatus.BAD_REQUEST.value()
            );
        }

        Page<Proveedor> providers = providerRepository.findAllByOrderByNombreNegocioAscIdAsc(
            PageRequest.of(page, size)
        );
        List<ProviderSummaryResponse> content = providers.getContent().stream()
            .map(ServiceCatalogService::toProviderSummary)
            .toList();

        return new ProviderPageResponse(
            content,
            providers.getNumber(),
            providers.getSize(),
            providers.getTotalElements(),
            providers.getTotalPages(),
            providers.isFirst(),
            providers.isLast(),
            providers.isEmpty()
        );
    }

    @Transactional(readOnly = true)
    public List<ProviderServiceResponse> getProviderServices(Integer providerId) {
        requirePositiveId(providerId, "providerId");
        if (!providerRepository.existsById(providerId)) {
            throw new BusinessRuleException(
                "PROVIDER_NOT_FOUND",
                "El proveedor solicitado no existe.",
                HttpStatus.NOT_FOUND.value()
            );
        }

        return serviceRepository.findAllByProveedor_IdOrderByNombreAscIdAsc(providerId)
                .stream()
                .map(service -> toProviderService(
                        service,
                        agendaRepository.findByServicio_Id(service.getId())
                                .map(agenda -> agenda.getId())
                                .orElse(null)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Integer serviceId) {
        requirePositiveId(serviceId, "serviceId");
        Servicio service = serviceRepository.findById(serviceId)
            .filter(servicio -> !servicio.isEliminado())
            .orElseThrow(() -> new BusinessRuleException(
                "SERVICE_NOT_FOUND",
                "El servicio solicitado no existe.",
                HttpStatus.NOT_FOUND.value()
            ));
        validateService(service);

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate startDate = now.toLocalDate();
        LocalDate endDate = startDate.plusDays(AVAILABILITY_DAYS - 1L);
        Integer providerId = service.getProveedor().getId();
        List<HorarioDisponible> schedules =
            scheduleRepository.findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(
                providerId
            );
        if (schedules.isEmpty()) {
            return List.of();
        }

        List<Reserva> reservations =
            reservationRepository.findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                providerId,
                startDate,
                endDate,
                ACTIVE_RESERVATION
            );
        reservations.forEach(ServiceCatalogService::validateActiveReservation);
        List<Recurso> activeResources = service.getRecursosCompatibles() == null
            ? List.of()
            : service.getRecursosCompatibles()
                .stream()
                .filter(Objects::nonNull)
                .filter(recurso ->
                    recurso.getId() != null
                        && recurso.isActivo()
                        && recurso.getProveedor() != null
                        && providerId.equals(recurso.getProveedor().getId())
                )
                .toList();
        if (activeResources.isEmpty()) {
            return List.of();
        }

        return schedules.stream()
            .flatMap(schedule ->
                availableSlots(
                    schedule,
                    service,
                    activeResources,
                    reservations,
                    startDate,
                    endDate,
                    now
                ).stream()
            )
            .distinct()
            .sorted((first, second) -> {
                int dateOrder = first.fecha().compareTo(second.fecha());
                if (dateOrder != 0) {
                    return dateOrder;
                }
                int timeOrder = first.hora().compareTo(second.hora());
                return timeOrder == 0
                    ? first.horarioId().compareTo(second.horarioId())
                    : timeOrder;
            })
            .toList();
    }

    private List<AvailableSlotResponse> availableSlots(
        HorarioDisponible schedule,
        Servicio service,
        List<Recurso> activeResources,
        List<Reserva> reservations,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime now
    ) {
        validateSchedule(schedule);
        if (!belongsToService(schedule, service)) {
            return List.of();
        }
        int intervalMinutes = schedule.getDuracionSlotMin();
        int serviceDurationMinutes = service.getDuracionMinutos();
        long scheduleDurationMinutes = Duration.between(
            schedule.getHoraInicio(),
            schedule.getHoraFin()
        ).toMinutes();
        if (
            intervalMinutes > scheduleDurationMinutes ||
            serviceDurationMinutes > scheduleDurationMinutes
        ) {
            return List.of();
        }

        return startDate.datesUntil(endDate.plusDays(1))
            .filter(date -> date.getDayOfWeek().getValue() - 1 == schedule.getDiaSemana())
            .flatMap(date -> {
                ArrayList<AvailableSlotResponse> slots = new ArrayList<>();
                for (
                    LocalTime time = schedule.getHoraInicio();
                    !time.plusMinutes(intervalMinutes).isAfter(schedule.getHoraFin());
                    time = time.plusMinutes(intervalMinutes)
                ) {
                    if (time.plusMinutes(serviceDurationMinutes).isAfter(schedule.getHoraFin())) {
                        continue;
                    }
                    LocalTime endTime = time.plusMinutes(serviceDurationMinutes);
                    if (
                        LocalDateTime.of(date, time).isAfter(now)
                            && hasAvailableResource(
                                activeResources,
                                reservations,
                                date,
                                time,
                                endTime
                            )
                    ) {
                        slots.add(new AvailableSlotResponse(schedule.getId(), date, time));
                    }
                }
                return slots.stream();
            })
            .toList();
    }

    private static boolean belongsToService(HorarioDisponible schedule, Servicio service) {
        return schedule.getAgenda() != null
            && schedule.getAgenda().isActiva()
            && schedule.getAgenda().getServicio() != null
            && Objects.equals(schedule.getAgenda().getServicio().getId(), service.getId())
            && schedule.getAgenda().getProveedor() != null
            && Objects.equals(
                schedule.getAgenda().getProveedor().getId(),
                service.getProveedor().getId()
            );
    }

    private static boolean hasAvailableResource(
        List<Recurso> resources,
        List<Reserva> reservations,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    ) {
        return resources.stream()
            .anyMatch(resource ->
                reservations.stream().noneMatch(reservation ->
                    reservation != null
                        && reservation.getEstado() != null
                        && ACTIVE_RESERVATION.equalsIgnoreCase(reservation.getEstado())
                        && date.equals(reservation.getFecha())
                        && reservation.getRecurso() != null
                        && resource.getId().equals(reservation.getRecurso().getId())
                        && reservation.getHora() != null
                        && reservation.getHoraFin() != null
                        && startTime.isBefore(reservation.getHoraFin())
                        && endTime.isAfter(reservation.getHora())
                )
            );
    }

    private static ProviderSummaryResponse toProviderSummary(
        Proveedor provider
    ) {
        if (provider == null || provider.getId() == null || provider.getNombreNegocio() == null) {
            throw new IllegalStateException(
                "A stored provider must have an identifier and business name."
            );
        }
        return new ProviderSummaryResponse(provider.getId(), provider.getNombreNegocio());
    }

    private static ProviderServiceResponse toProviderService(Servicio service, Integer agendaId) {
        if (
            service == null ||
            service.getId() == null ||
            service.getProveedor() == null ||
            service.getProveedor().getNombreNegocio() == null ||
            service.getDuracionMinutos() == null ||
            service.getDuracionMinutos() <= 0
        ) {
            throw new IllegalStateException(
                "A stored service must have an identifier, provider and positive duration."
            );
        }
        return new ProviderServiceResponse(
            service.getId(),
            service.getNombre(),
            service.getDuracionMinutos(),
            service.getProveedor().getNombreNegocio(),
                agendaId
        );
    }

    private static void validateService(Servicio service) {
        if (
            service.getId() == null ||
            service.getProveedor() == null ||
            service.getProveedor().getId() == null ||
            service.getNombre() == null ||
            service.getDuracionMinutos() == null ||
            service.getDuracionMinutos() <= 0
        ) {
            throw new IllegalStateException(
                "A stored service must have a provider and a positive duration."
            );
        }
    }

    private static void validateSchedule(HorarioDisponible schedule) {
        if (
            schedule == null ||
            schedule.getId() == null ||
            schedule.getDiaSemana() == null ||
            schedule.getDiaSemana() < 0 ||
            schedule.getDiaSemana() > 6 ||
            schedule.getHoraInicio() == null ||
            schedule.getHoraFin() == null ||
            !schedule.getHoraFin().isAfter(schedule.getHoraInicio()) ||
            schedule.getDuracionSlotMin() == null ||
            schedule.getDuracionSlotMin() <= 0
        ) {
            throw new IllegalStateException(
                "A stored schedule must have a valid day, time range and positive slot duration."
            );
        }
    }

    private static void validateActiveReservation(Reserva reservation) {
        if (
            reservation == null ||
            reservation.getHorario() == null ||
            reservation.getHorario().getId() == null ||
            reservation.getFecha() == null
        ) {
            throw new IllegalStateException(
                "A stored active reservation must reference a schedule and date."
            );
        }
    }

    private static void requirePositiveId(Integer id, String parameterName) {
        if (id == null || id < 1) {
            throw new BusinessRuleException(
                "INVALID_PARAMETER",
                "El parámetro '" + parameterName + "' debe ser un entero positivo.",
                HttpStatus.BAD_REQUEST.value()
            );
        }
    }
}
