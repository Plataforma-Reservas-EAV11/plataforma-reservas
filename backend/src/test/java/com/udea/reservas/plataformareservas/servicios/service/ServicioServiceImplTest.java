package com.udea.reservas.plataformareservas.servicios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.recursos.repository.RecursoRepository;
import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ServicioServiceImplTest {

    @Mock
    private IProveedorRepository proveedorRepository;

    @Mock
    private IServicioRepository servicioRepository;

    @Mock
    private IAgendaRepository agendaRepository;

    @Mock
    private RecursoRepository recursoRepository;

    @InjectMocks
    private ServicioServiceImpl servicioService;

    @Test
    @DisplayName("Crea el servicio para el único perfil del usuario autenticado")
    void crearServicioAsociaElPerfilDelUsuarioAutenticado() {
        Proveedor proveedor = Proveedor.builder().id(12).build();
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of(proveedor));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(invocation -> {
            Servicio servicio = invocation.getArgument(0);
            servicio.setId(41);
            return servicio;
        });
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(invocation -> {
            Agenda agenda = invocation.getArgument(0);
            agenda.setId(42);
            return agenda;
        });

        ServicioResponse response = servicioService.crearServicio(
            7,
            new CrearServicioRequest("Corte", "Corte de cabello", 30)
        );

        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);
        verify(servicioRepository).save(captor.capture());
        assertThat(captor.getValue().getProveedor()).isSameAs(proveedor);
        assertThat(captor.getValue().getNombre()).isEqualTo("Corte");
        assertThat(captor.getValue().getDescripcion()).isEqualTo("Corte de cabello");
        assertThat(captor.getValue().getDuracionMinutos()).isEqualTo(30);
        ArgumentCaptor<Agenda> agendaCaptor = ArgumentCaptor.forClass(Agenda.class);
        verify(agendaRepository).save(agendaCaptor.capture());
        assertThat(agendaCaptor.getValue().getProveedor()).isSameAs(proveedor);
        assertThat(agendaCaptor.getValue().getServicio()).isSameAs(captor.getValue());
        assertThat(response).isEqualTo(
            new ServicioResponse(41, 42, "Corte", "Corte de cabello", 30)
        );
    }

    @Test
    @DisplayName("Rechaza un usuario sin perfil de proveedor sin guardar")
    void crearServicioSinPerfilProveedorNoGuarda() {
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of());
        CrearServicioRequest request = new CrearServicioRequest(
            "Corte",
            "Descripción",
            30
        );

        assertThatThrownBy(() ->
            servicioService.crearServicio(7, request)
        )
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                BusinessRuleException businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode()).isEqualTo("PROVIDER_PROFILE_NOT_FOUND");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });

        verify(servicioRepository, never()).save(any(Servicio.class));
        verify(agendaRepository, never()).save(any(Agenda.class));
    }

    @Test
    @DisplayName("Rechaza perfiles de proveedor duplicados sin elegir uno arbitrariamente")
    void crearServicioConPerfilesDuplicadosNoGuarda() {
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(
            List.of(Proveedor.builder().id(12).build(), Proveedor.builder().id(13).build())
        );
        CrearServicioRequest request = new CrearServicioRequest(
            "Corte",
            "Descripción",
            30
        );

        assertThatThrownBy(() ->
            servicioService.crearServicio(7, request)
        )
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                BusinessRuleException businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode())
                    .isEqualTo("PROVIDER_PROFILE_AMBIGUOUS");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });

        verify(servicioRepository, never()).save(any(Servicio.class));
        verify(agendaRepository, never()).save(any(Agenda.class));
    }

    @Test
    @DisplayName("Permite crear servicios con nombres duplicados para el mismo proveedor")
    void crearServicioPermiteNombresDuplicados() {
        Proveedor proveedor = Proveedor.builder().id(12).build();
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of(proveedor));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(invocation -> {
            Servicio servicio = invocation.getArgument(0);
            servicio.setId(41);
            return servicio;
        });
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(invocation -> {
            Agenda agenda = invocation.getArgument(0);
            agenda.setId(42);
            return agenda;
        });
        CrearServicioRequest request = new CrearServicioRequest("Corte", "Descripción", 30);

        servicioService.crearServicio(7, request);
        servicioService.crearServicio(7, request);

        verify(servicioRepository, times(2)).save(any(Servicio.class));
    }

    @Test
    @DisplayName("Asocia únicamente los recursos solicitados que pertenecen al proveedor")
    void crearServicioAsociaRecursosDelProveedor() {
        Proveedor proveedor = Proveedor.builder().id(12).build();
        Recurso recurso = Recurso.builder()
            .id(51)
            .proveedor(proveedor)
            .nombre("Consultorio 1")
            .activo(true)
            .build();
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of(proveedor));
        when(recursoRepository.findAllByIdInAndProveedor_Id(List.of(51), 12))
            .thenReturn(List.of(recurso));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(invocation -> {
            Servicio servicio = invocation.getArgument(0);
            servicio.setId(41);
            return servicio;
        });
        when(agendaRepository.save(any(Agenda.class))).thenAnswer(invocation -> {
            Agenda agenda = invocation.getArgument(0);
            agenda.setId(42);
            return agenda;
        });

        ServicioResponse response = servicioService.crearServicio(
            7,
            new CrearServicioRequest("Consulta", "Consulta general", 30, List.of(51))
        );

        ArgumentCaptor<Servicio> captor = ArgumentCaptor.forClass(Servicio.class);
        verify(servicioRepository).save(captor.capture());
        assertThat(captor.getValue().getRecursosCompatibles()).containsExactly(recurso);
        assertThat(response.recursoIds()).containsExactly(51);
    }

    @Test
    @DisplayName("Rechaza recursos de otro proveedor y no crea servicio ni agenda")
    void crearServicioRechazaRecursosAjenoProveedor() {
        Proveedor proveedor = Proveedor.builder().id(12).build();
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of(proveedor));
        when(recursoRepository.findAllByIdInAndProveedor_Id(List.of(51), 12)).thenReturn(List.of());

        assertThatThrownBy(() ->
            servicioService.crearServicio(
                7,
                new CrearServicioRequest("Consulta", "Consulta general", 30, List.of(51))
            )
        )
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                BusinessRuleException businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode()).isEqualTo("INVALID_SERVICE_RESOURCES");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            });

        verify(servicioRepository, never()).save(any(Servicio.class));
        verify(agendaRepository, never()).save(any(Agenda.class));
    }
}
