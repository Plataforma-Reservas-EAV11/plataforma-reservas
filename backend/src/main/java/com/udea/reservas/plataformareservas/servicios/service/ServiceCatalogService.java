package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.common.BusinessRuleException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        CatalogDataValidator.validatePagination(page, size, PAGE_SIZE_MAX);

        Page<Proveedor> providers = providerRepository.findAllByOrderByNombreNegocioAscIdAsc(
            PageRequest.of(page, size)
        );
        List<ProviderSummaryResponse> content = providers.getContent().stream()
            .map(CatalogResponseMapper::toProviderSummary)
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
        CatalogDataValidator.requirePositiveId(providerId, "providerId");
        if (!providerRepository.existsById(providerId)) {
            throw new BusinessRuleException(
                "PROVIDER_NOT_FOUND",
                "El proveedor solicitado no existe.",
                HttpStatus.NOT_FOUND.value()
            );
        }

        return serviceRepository.findAllByProveedor_IdOrderByNombreAscIdAsc(providerId)
            .stream()
            .map(service ->
                CatalogResponseMapper.toProviderService(service, agendaIdFor(service))
            )
            .toList();
    }

    private Integer agendaIdFor(Servicio service) {
        return Optional.ofNullable(service)
            .map(Servicio::getId)
            .flatMap(agendaRepository::findByServicio_Id)
            .map(Agenda::getId)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Integer serviceId) {
        CatalogDataValidator.requirePositiveId(serviceId, "serviceId");
        Servicio service = findActiveService(serviceId);
        CatalogDataValidator.validateService(service);

        AvailabilityWindow window = AvailabilityWindow.startingAt(
            LocalDateTime.now(clock),
            AVAILABILITY_DAYS
        );
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
                window.startDate(),
                window.endDate(),
                ACTIVE_RESERVATION
            );
        reservations.forEach(CatalogDataValidator::validateActiveReservation);
        List<Recurso> activeResources = AvailabilityCalculator.activeResourcesFor(
            service,
            providerId
        );
        if (activeResources.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, Map<Integer, List<Reserva>>> reservationsByDate =
            AvailabilityCalculator.indexActiveReservations(reservations);

        return schedules.stream()
            .flatMap(schedule ->
                AvailabilityCalculator.slotsFor(
                    schedule,
                    service,
                    activeResources,
                    reservationsByDate,
                    window
                ).stream()
            )
            .distinct()
            .sorted(AvailabilityCalculator.slotOrder())
            .toList();
    }

    private Servicio findActiveService(Integer serviceId) {
        return serviceRepository.findById(serviceId)
            .filter(servicio -> !servicio.isEliminado())
            .orElseThrow(() -> new BusinessRuleException(
                "SERVICE_NOT_FOUND",
                "El servicio solicitado no existe.",
                HttpStatus.NOT_FOUND.value()
            ));
    }
}
