package com.udea.reservas.plataformareservas.servicios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.udea.reservas.plataformareservas.servicios.dto.ProviderSummaryResponse;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    private static final Integer PROVIDER_ID = 4;
    private static final Integer SERVICE_ID = 12;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);

    @Mock
    private IProveedorRepository providerRepository;

    @Mock
    private IServicioRepository serviceRepository;

    @Mock
    private IHorarioDisponibleRepository scheduleRepository;

    @Mock
    private IReservaRepository reservationRepository;

    @Mock
    private IAgendaRepository agendaRepository;

    @InjectMocks
    private ServiceCatalogService catalogService;

    @BeforeEach
    void setClock() {
        catalogService = new ServiceCatalogService(
            providerRepository,
            serviceRepository,
            scheduleRepository,
            reservationRepository,
            agendaRepository,
            Clock.fixed(
                Instant.parse("2026-11-05T15:00:00Z"),
                ZoneId.of("America/Bogota")
            )
        );
    }

    @Test
    @DisplayName("Lista proveedores paginados con nombre público únicamente")
    void listsProvidersAsPage() {
        Proveedor provider = Proveedor.builder()
            .id(PROVIDER_ID)
            .nombreNegocio("Salón Central")
            .telefono("3001234567")
            .build();
        when(providerRepository.findAllByOrderByNombreNegocioAscIdAsc(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(provider), PageRequest.of(
                0,
                1
            ), 2));

        var response = catalogService.getProviders(0, 1);

        assertThat(response.content())
            .containsExactly(new ProviderSummaryResponse(PROVIDER_ID, "Salón Central"));
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Devuelve una página vacía cuando no hay proveedores")
    void returnsEmptyProviderPage() {
        when(providerRepository.findAllByOrderByNombreNegocioAscIdAsc(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of()));

        assertThat(catalogService.getProviders(0, 20).empty()).isTrue();
    }

    @Test
    @DisplayName("Rechaza valores de paginación fuera de rango")
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> catalogService.getProviders(-1, 20))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> catalogService.getProviders(0, 0))
            .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> catalogService.getProviders(0, 101))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Devuelve lista vacía al consultar un proveedor sin servicios")
    void returnsEmptyServicesForProvider() {
        when(providerRepository.existsById(PROVIDER_ID)).thenReturn(true);
        when(serviceRepository.findAllByProveedor_IdOrderByNombreAscIdAsc(PROVIDER_ID))
            .thenReturn(List.of());

        assertThat(catalogService.getProviderServices(PROVIDER_ID)).isEmpty();
    }

    @Test
    @DisplayName("Devuelve sólo los servicios obtenidos para el proveedor solicitado")
    void listsServicesForRequestedProvider() {
        Proveedor provider = Proveedor.builder()
            .id(PROVIDER_ID)
            .nombreNegocio("Salón Central")
            .build();
        Servicio service = Servicio.builder()
            .id(SERVICE_ID)
            .proveedor(provider)
            .nombre("Corte")
            .duracionMinutos(30)
            .build();
        when(providerRepository.existsById(PROVIDER_ID)).thenReturn(true);
        when(serviceRepository.findAllByProveedor_IdOrderByNombreAscIdAsc(PROVIDER_ID))
            .thenReturn(List.of(service));
        when(agendaRepository.findByServicio_Id(SERVICE_ID))
            .thenReturn(Optional.of(Agenda.builder().id(77).servicio(service).build()));

        var response = catalogService.getProviderServices(PROVIDER_ID);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().nombre()).isEqualTo("Corte");
        assertThat(response.getFirst().duracionMinutos()).isEqualTo(30);
        assertThat(response.getFirst().proveedorNombre()).isEqualTo("Salón Central");
        assertThat(response.getFirst().agendaId()).isEqualTo(77);
        verify(serviceRepository).findAllByProveedor_IdOrderByNombreAscIdAsc(PROVIDER_ID);
    }

    @Test
    @DisplayName("Responde proveedor inexistente sin consultar servicios")
    void rejectsUnknownProvider() {
        when(providerRepository.existsById(PROVIDER_ID)).thenReturn(false);

        assertThatThrownBy(() -> catalogService.getProviderServices(PROVIDER_ID))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Rechaza un identificador de proveedor nulo o no positivo antes de consultar")
    void rejectsInvalidProviderIds() {
        assertThatThrownBy(() -> catalogService.getProviderServices(null))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("errorCode")
            .isEqualTo("INVALID_PARAMETER");
        assertThatThrownBy(() -> catalogService.getProviderServices(0))
            .isInstanceOf(BusinessRuleException.class);
        org.mockito.Mockito.verifyNoInteractions(providerRepository, serviceRepository);
    }

    @Test
    @DisplayName("Detecta una respuesta del repositorio con un proveedor nulo")
    void rejectsNullProviderReturnedFromRepository() {
        when(providerRepository.findAllByOrderByNombreNegocioAscIdAsc(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(java.util.Arrays.asList((Proveedor) null)));

        assertThatThrownBy(() -> catalogService.getProviders(0, 20))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("stored provider");
    }

    @Test
    @DisplayName("Detecta proveedores persistidos incompletos al construir la página")
    void rejectsProvidersWithoutIdentifierOrBusinessName() {
        when(providerRepository.findAllByOrderByNombreNegocioAscIdAsc(any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(
                Proveedor.builder().nombreNegocio("Salón").build()
            )))
            .thenReturn(new PageImpl<>(List.of(
                Proveedor.builder().id(PROVIDER_ID).build()
            )));

        assertThatThrownBy(() -> catalogService.getProviders(0, 20))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> catalogService.getProviders(0, 20))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Detecta servicios persistidos incompletos al listar un proveedor")
    void rejectsIncompleteProviderServices() {
        List<Servicio> invalidServices = java.util.Arrays.asList(
            Servicio.builder().proveedor(Proveedor.builder().nombreNegocio("Salón").build())
                .duracionMinutos(30).build(),
            Servicio.builder().id(SERVICE_ID).duracionMinutos(30).build(),
            Servicio.builder().id(SERVICE_ID)
                .proveedor(Proveedor.builder().id(PROVIDER_ID).build())
                .duracionMinutos(30).build(),
            Servicio.builder().id(SERVICE_ID)
                .proveedor(Proveedor.builder().id(PROVIDER_ID).nombreNegocio("Salón").build())
                .build(),
            Servicio.builder().id(SERVICE_ID)
                .proveedor(Proveedor.builder().id(PROVIDER_ID).nombreNegocio("Salón").build())
                .duracionMinutos(0).build(),
            null
        );
        when(providerRepository.existsById(PROVIDER_ID)).thenReturn(true);

        for (Servicio invalidService : invalidServices) {
            when(serviceRepository.findAllByProveedor_IdOrderByNombreAscIdAsc(PROVIDER_ID))
                .thenReturn(java.util.Arrays.asList(invalidService));
            assertThatThrownBy(() -> catalogService.getProviderServices(PROVIDER_ID))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("Excluye fechas pasadas y devuelve los turnos futuros en orden")
    void excludesPastSlots() {
        Servicio service = service();
        HorarioDisponible schedule = schedule(51, 3);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(schedule));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                PROVIDER_ID,
                TODAY,
                TODAY.plusDays(29),
                "activa"
            ))
            .thenReturn(List.of());

        var slots = catalogService.getAvailableSlots(SERVICE_ID);

        assertThat(slots)
            .filteredOn(slot -> slot.fecha().equals(TODAY))
            .extracting(slot -> slot.hora())
            .doesNotContain(LocalTime.of(8, 0), LocalTime.of(10, 0))
            .contains(LocalTime.of(10, 30), LocalTime.of(11, 0), LocalTime.of(11, 30));
        assertThat(slots).isSortedAccordingTo((first, second) -> {
            int dateOrder = first.fecha().compareTo(second.fecha());
            return dateOrder == 0
                ? first.hora().compareTo(second.hora())
                : dateOrder;
        });
    }

    @Test
    @DisplayName("Excluye solo turnos solapados para el recurso reservado")
    void excludesOnlyOverlappingSlotsForTheReservedResource() {
        Servicio service = service();
        service.setRecursosCompatibles(List.of(resource(30)));
        HorarioDisponible schedule = schedule(51, 3);
        Reserva reservation = Reserva.builder()
            .horario(schedule)
            .fecha(TODAY)
            .hora(LocalTime.of(11, 0))
            .horaFin(LocalTime.of(11, 30))
            .recurso(resource(30))
            .estado("activa")
            .build();
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(schedule));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                PROVIDER_ID,
                TODAY,
                TODAY.plusDays(29),
                "activa"
            ))
            .thenReturn(List.of(reservation));

        var slots = catalogService.getAvailableSlots(SERVICE_ID);

        assertThat(slots)
            .filteredOn(slot -> slot.fecha().equals(TODAY))
            .extracting(slot -> slot.hora())
            .contains(LocalTime.of(10, 30), LocalTime.of(11, 30))
            .doesNotContain(LocalTime.of(11, 0));
        assertThat(slots).anyMatch(slot -> slot.fecha().equals(TODAY.plusDays(7)));
    }

    @Test
    @DisplayName("Mantiene el turno si otro recurso compatible sigue libre")
    void keepsSlotAvailableWhenAnotherCompatibleResourceIsFree() {
        Servicio service = service();
        HorarioDisponible schedule = schedule(51, 3);
        Reserva reservation = Reserva.builder()
            .horario(schedule)
            .fecha(TODAY)
            .hora(LocalTime.of(11, 0))
            .horaFin(LocalTime.of(11, 30))
            .recurso(resource(30))
            .estado("activa")
            .build();
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(schedule));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                PROVIDER_ID,
                TODAY,
                TODAY.plusDays(29),
                "activa"
            ))
            .thenReturn(List.of(reservation));

        assertThat(catalogService.getAvailableSlots(SERVICE_ID))
            .anyMatch(slot ->
                slot.fecha().equals(TODAY) && slot.hora().equals(LocalTime.of(11, 0))
            );
    }

    @Test
    @DisplayName("No devuelve horarios de otra agenda del mismo proveedor")
    void excludesSchedulesForOtherServices() {
        HorarioDisponible otherServiceSchedule = schedule(51, 3);
        otherServiceSchedule.getAgenda().setServicio(
            Servicio.builder().id(SERVICE_ID + 1).build()
        );
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service()));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(otherServiceSchedule));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                any(),
                any(),
                any(),
                any()
            ))
            .thenReturn(List.of());

        assertThat(catalogService.getAvailableSlots(SERVICE_ID)).isEmpty();
    }

    @Test
    @DisplayName("No devuelve turnos de agendas desactivadas")
    void excludesDisabledAgendas() {
        HorarioDisponible disabledSchedule = schedule(51, 3);
        disabledSchedule.getAgenda().setActiva(false);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service()));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(disabledSchedule));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                any(),
                any(),
                any(),
                any()
            ))
            .thenReturn(List.of());

        assertThat(catalogService.getAvailableSlots(SERVICE_ID)).isEmpty();
        verify(reservationRepository)
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                any(),
                any(),
                any(),
                any()
            );
    }

    @Test
    @DisplayName("Devuelve lista vacía si el servicio no tiene horarios")
    void returnsEmptyWhenServiceHasNoSchedules() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service()));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of());

        assertThat(catalogService.getAvailableSlots(SERVICE_ID)).isEmpty();
        verify(reservationRepository, never())
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                any(),
                any(),
                any(),
                any()
            );
    }

    @Test
    @DisplayName("Responde servicio inexistente")
    void rejectsUnknownService() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getAvailableSlots(SERVICE_ID))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("status")
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Rechaza un identificador de servicio nulo antes de consultar")
    void rejectsNullServiceId() {
        assertThatThrownBy(() -> catalogService.getAvailableSlots(null))
            .isInstanceOf(BusinessRuleException.class)
            .extracting("errorCode")
            .isEqualTo("INVALID_PARAMETER");
        verify(serviceRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Detecta servicio sin proveedor en los datos persistidos")
    void rejectsServiceWithoutProvider() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(
            Optional.of(Servicio.builder()
                .id(SERVICE_ID)
                .nombre("Corte")
                .duracionMinutos(30)
                .build())
        );

        assertThatThrownBy(() -> catalogService.getAvailableSlots(SERVICE_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("positive duration");
        verifyNoScheduleQueries();
    }

    @Test
    @DisplayName("Detecta datos obligatorios incompletos en un servicio almacenado")
    void rejectsIncompleteStoredService() {
        Proveedor providerWithoutId = Proveedor.builder().nombreNegocio("Salón").build();
        List<Servicio> invalidServices = List.of(
            Servicio.builder()
                .proveedor(Proveedor.builder().id(PROVIDER_ID).build())
                .nombre("Corte")
                .duracionMinutos(30)
                .build(),
            Servicio.builder()
                .id(SERVICE_ID)
                .proveedor(providerWithoutId)
                .nombre("Corte")
                .duracionMinutos(30)
                .build(),
            Servicio.builder()
                .id(SERVICE_ID)
                .proveedor(Proveedor.builder().id(PROVIDER_ID).build())
                .duracionMinutos(30)
                .build(),
            Servicio.builder()
                .id(SERVICE_ID)
                .proveedor(Proveedor.builder().id(PROVIDER_ID).build())
                .nombre("Corte")
                .build(),
            Servicio.builder()
                .id(SERVICE_ID)
                .proveedor(Proveedor.builder().id(PROVIDER_ID).build())
                .nombre("Corte")
                .duracionMinutos(0)
                .build()
        );

        for (Servicio invalidService : invalidServices) {
            when(serviceRepository.findById(SERVICE_ID))
                .thenReturn(Optional.of(invalidService));
            assertThatThrownBy(() -> catalogService.getAvailableSlots(SERVICE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stored service");
        }
        verifyNoScheduleQueries();
    }

    @Test
    @DisplayName("Detecta bloques persistidos con duración de slot inválida")
    void rejectsInvalidStoredSchedule() {
        HorarioDisponible invalidSchedule = schedule(51, 3);
        invalidSchedule.setDuracionSlotMin(0);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service()));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(invalidSchedule));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                any(),
                any(),
                any(),
                any()
            ))
            .thenReturn(List.of());

        assertThatThrownBy(() -> catalogService.getAvailableSlots(SERVICE_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("valid day, time range");
    }

    @Test
    @DisplayName("Detecta todos los campos obligatorios incompletos en un horario")
    void rejectsIncompleteStoredSchedules() {
        List<HorarioDisponible> invalidSchedules = java.util.Arrays.asList(
            null,
            HorarioDisponible.builder().diaSemana(3).horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0)).duracionSlotMin(30).build(),
            scheduleWith(null, LocalTime.of(8, 0), LocalTime.of(12, 0), 30),
            scheduleWith(-1, LocalTime.of(8, 0), LocalTime.of(12, 0), 30),
            scheduleWith(7, LocalTime.of(8, 0), LocalTime.of(12, 0), 30),
            scheduleWith(3, null, LocalTime.of(12, 0), 30),
            scheduleWith(3, LocalTime.of(8, 0), null, 30),
            scheduleWith(3, LocalTime.of(12, 0), LocalTime.of(8, 0), 30),
            scheduleWith(3, LocalTime.of(8, 0), LocalTime.of(12, 0), null),
            scheduleWith(3, LocalTime.of(8, 0), LocalTime.of(12, 0), 0)
        );
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service()));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                any(),
                any(),
                any(),
                any()
            ))
            .thenReturn(List.of());

        for (HorarioDisponible invalidSchedule : invalidSchedules) {
            when(scheduleRepository
                .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
                .thenReturn(java.util.Arrays.asList(invalidSchedule));
            assertThatThrownBy(() -> catalogService.getAvailableSlots(SERVICE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("valid day, time range");
        }
    }

    @Test
    @DisplayName("Devuelve vacío cuando el intervalo excede el bloque sin desbordar la hora")
    void returnsEmptyWhenSlotIntervalIsLongerThanSchedule() {
        HorarioDisponible schedule = schedule(51, 3);
        schedule.setDuracionSlotMin(1_440);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service()));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(schedule));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                any(),
                any(),
                any(),
                any()
            ))
            .thenReturn(List.of());

        assertThat(catalogService.getAvailableSlots(SERVICE_ID)).isEmpty();
    }

    @Test
    @DisplayName("Detecta reservas activas persistidas con datos incompletos")
    void rejectsIncompleteActiveReservations() {
        HorarioDisponible validSchedule = schedule(51, 3);
        List<Reserva> invalidReservations = java.util.Arrays.asList(
            null,
            Reserva.builder().fecha(TODAY).build(),
            Reserva.builder()
                .horario(HorarioDisponible.builder().build())
                .fecha(TODAY)
                .build(),
            Reserva.builder().horario(validSchedule).build()
        );
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service()));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(validSchedule));

        for (Reserva invalidReservation : invalidReservations) {
            when(reservationRepository
                .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                    any(),
                    any(),
                    any(),
                    any()
                ))
                .thenReturn(java.util.Arrays.asList(invalidReservation));
            assertThatThrownBy(() -> catalogService.getAvailableSlots(SERVICE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active reservation");
        }
    }

    @Test
    @DisplayName("Devuelve vacío cuando el servicio dura más que el bloque de agenda")
    void returnsEmptyWhenServiceIsLongerThanSchedule() {
        HorarioDisponible schedule = schedule(51, 3);
        when(serviceRepository.findById(SERVICE_ID))
            .thenReturn(Optional.of(serviceWithDuration(300)));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(schedule));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                any(),
                any(),
                any(),
                any()
            ))
            .thenReturn(List.of());

        assertThat(catalogService.getAvailableSlots(SERVICE_ID)).isEmpty();
    }

    @Test
    @DisplayName("No propone un inicio si la duración del servicio excede el fin del bloque")
    void excludesSlotsThatCannotFitTheServiceDuration() {
        HorarioDisponible schedule = schedule(51, 3);
        when(serviceRepository.findById(SERVICE_ID))
            .thenReturn(Optional.of(serviceWithDuration(60)));
        when(scheduleRepository
            .findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(schedule));
        when(reservationRepository
            .findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
                any(),
                any(),
                any(),
                any()
            ))
            .thenReturn(List.of());

        var slots = catalogService.getAvailableSlots(SERVICE_ID);

        assertThat(slots)
            .filteredOn(slot -> slot.fecha().equals(TODAY))
            .extracting(slot -> slot.hora())
            .contains(LocalTime.of(11, 0))
            .doesNotContain(LocalTime.of(11, 30));
    }

    @Test
    void deletedServiceHasNoAvailability() {
        Servicio deleted = service();
        deleted.setEliminado(true);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(deleted));
        assertThatThrownBy(() -> catalogService.getAvailableSlots(SERVICE_ID))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                var error = (BusinessRuleException) exception;
                assertThat(error.getErrorCode()).isEqualTo("SERVICE_NOT_FOUND");
                assertThat(error.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
        verifyNoScheduleQueries();
    }

    @Test
    void missingOrInvalidResourcesCannotOfferSlots() {
        Servicio service = service();
        HorarioDisponible schedule = schedule(51, 3);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(scheduleRepository.findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(schedule));
        when(reservationRepository.findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
            any(), any(), any(), any())).thenReturn(List.of());

        service.setRecursosCompatibles(null);
        assertThat(catalogService.getAvailableSlots(SERVICE_ID)).isEmpty();
        service.setRecursosCompatibles(java.util.Arrays.asList(
            null,
            resource(null),
            Recurso.builder().id(1).activo(false).proveedor(Proveedor.builder().id(PROVIDER_ID).build()).build(),
            Recurso.builder().id(2).activo(true).build(),
            Recurso.builder().id(3).activo(true).proveedor(Proveedor.builder().id(99).build()).build()
        ));
        assertThat(catalogService.getAvailableSlots(SERVICE_ID)).isEmpty();
    }

    @Test
    void schedulesMustHaveMatchingActiveServiceAndProvider() {
        Servicio service = service();
        List<HorarioDisponible> schedules = new java.util.ArrayList<>();
        HorarioDisponible noAgenda = schedule(51, 3);
        noAgenda.setAgenda(null);
        schedules.add(noAgenda);
        HorarioDisponible noService = schedule(52, 3);
        noService.getAgenda().setServicio(null);
        schedules.add(noService);
        HorarioDisponible noProvider = schedule(53, 3);
        noProvider.getAgenda().setProveedor(null);
        schedules.add(noProvider);
        HorarioDisponible otherProvider = schedule(54, 3);
        otherProvider.getAgenda().setProveedor(Proveedor.builder().id(99).build());
        schedules.add(otherProvider);
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(scheduleRepository.findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(schedules);
        when(reservationRepository.findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
            any(), any(), any(), any())).thenReturn(List.of());

        assertThat(catalogService.getAvailableSlots(SERVICE_ID)).isEmpty();
    }

    @Test
    void simultaneousSlotsAreOrderedByScheduleIdentifier() {
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service()));
        when(scheduleRepository.findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(schedule(52, 3), schedule(51, 3)));
        when(reservationRepository.findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
            any(), any(), any(), any())).thenReturn(List.of());

        assertThat(catalogService.getAvailableSlots(SERVICE_ID))
            .filteredOn(slot -> slot.fecha().equals(TODAY) && slot.hora().equals(LocalTime.of(10, 30)))
            .extracting(slot -> slot.horarioId())
            .containsExactly(51, 52);
    }

    @Test
    void nonBlockingReservationsDoNotHideAvailableSlots() {
        HorarioDisponible schedule = schedule(51, 3);
        Servicio service = service();
        service.setRecursosCompatibles(List.of(resource(30)));
        List<Reserva> reservations = List.of(
            Reserva.builder().horario(schedule).fecha(TODAY).estado(null).build(),
            Reserva.builder().horario(schedule).fecha(TODAY).estado("cancelada").build(),
            Reserva.builder().horario(schedule).fecha(TODAY.plusDays(1)).estado("activa").build(),
            Reserva.builder().horario(schedule).fecha(TODAY).estado("activa").build(),
            Reserva.builder().horario(schedule).fecha(TODAY).estado("activa").recurso(resource(31)).build(),
            Reserva.builder().horario(schedule).fecha(TODAY).estado("activa").recurso(resource(30)).build(),
            Reserva.builder().horario(schedule).fecha(TODAY).estado("activa").recurso(resource(30))
                .hora(LocalTime.of(10, 30)).build()
        );
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(scheduleRepository.findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(PROVIDER_ID))
            .thenReturn(List.of(schedule));
        when(reservationRepository.findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
            any(), any(), any(), any())).thenReturn(reservations);

        assertThat(catalogService.getAvailableSlots(SERVICE_ID))
            .anyMatch(slot -> slot.fecha().equals(TODAY) && slot.hora().equals(LocalTime.of(10, 30)));
    }

    private static Servicio service() {
        return serviceWithDuration(30);
    }

    private static Servicio serviceWithDuration(int duration) {
        return Servicio.builder()
            .id(SERVICE_ID)
            .proveedor(Proveedor.builder().id(PROVIDER_ID).build())
            .nombre("Corte")
            .duracionMinutos(duration)
            .recursosCompatibles(List.of(resource(30), resource(31)))
            .build();
    }

    private static Recurso resource(Integer resourceId) {
        return Recurso.builder()
            .id(resourceId)
            .proveedor(Proveedor.builder().id(PROVIDER_ID).build())
            .activo(true)
            .build();
    }

    private static HorarioDisponible schedule(Integer scheduleId, int weekday) {
        return HorarioDisponible.builder()
            .id(scheduleId)
            .agenda(Agenda.builder()
                .id(20)
                .proveedor(Proveedor.builder().id(PROVIDER_ID).build())
                .servicio(service())
                .activa(true)
                .build())
            .diaSemana(weekday)
            .horaInicio(LocalTime.of(8, 0))
            .horaFin(LocalTime.of(12, 0))
            .duracionSlotMin(30)
            .build();
    }

    private static HorarioDisponible scheduleWith(
        Integer weekday,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDuration
    ) {
        return HorarioDisponible.builder()
            .id(51)
            .agenda(Agenda.builder()
                .id(20)
                .proveedor(Proveedor.builder().id(PROVIDER_ID).build())
                .servicio(service())
                .activa(true)
                .build())
            .diaSemana(weekday)
            .horaInicio(startTime)
            .horaFin(endTime)
            .duracionSlotMin(slotDuration)
            .build();
    }

    private void verifyNoScheduleQueries() {
        org.mockito.Mockito.verifyNoInteractions(scheduleRepository, reservationRepository);
    }
}
