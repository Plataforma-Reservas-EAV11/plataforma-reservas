package com.udea.reservas.plataformareservas.servicios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
import com.udea.reservas.plataformareservas.reservas.repository.IReservaRepository;
import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import java.util.List;
import java.util.Optional;
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
    private IReservaRepository reservaRepository;

    @InjectMocks
    private ServicioServiceImpl servicioService;

    @Test
    @DisplayName("Elimina lógicamente un servicio sin reservas activas")
    void eliminarServicioSinReservasActivasLoDesactiva() {
        Proveedor proveedor = Proveedor.builder().id(12).build();
        Servicio servicio = Servicio.builder().id(41).proveedor(proveedor).build();
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of(proveedor));
        when(servicioRepository.findByIdAndProveedor_IdAndActivoTrue(41, 12))
            .thenReturn(Optional.of(servicio));
        when(reservaRepository.existsByServicio_IdAndEstadoIgnoreCase(41, "activa"))
            .thenReturn(false);

        servicioService.eliminarServicio(7, 41);

        assertThat(servicio.isActivo()).isFalse();
        verify(servicioRepository).save(servicio);
        verify(servicioRepository, never()).delete(any(Servicio.class));
        verify(reservaRepository, never()).save(any());
        verify(reservaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Rechaza eliminar un servicio con reservas activas y lo mantiene activo")
    void eliminarServicioConReservasActivasResponde409() {
        Proveedor proveedor = Proveedor.builder().id(12).build();
        Servicio servicio = Servicio.builder().id(41).proveedor(proveedor).build();
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of(proveedor));
        when(servicioRepository.findByIdAndProveedor_IdAndActivoTrue(41, 12))
            .thenReturn(Optional.of(servicio));
        when(reservaRepository.existsByServicio_IdAndEstadoIgnoreCase(41, "activa"))
            .thenReturn(true);

        assertThatThrownBy(() -> servicioService.eliminarServicio(7, 41))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                BusinessRuleException businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode())
                    .isEqualTo("SERVICE_HAS_ACTIVE_RESERVATIONS");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });

        assertThat(servicio.isActivo()).isTrue();
        verify(servicioRepository, never()).save(any(Servicio.class));
        verify(reservaRepository, never()).save(any());
        verify(reservaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Un servicio inexistente, ajeno o ya eliminado responde 404 sin cambios")
    void eliminarServicioNoEncontradoResponde404() {
        Proveedor proveedor = Proveedor.builder().id(12).build();
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of(proveedor));
        when(servicioRepository.findByIdAndProveedor_IdAndActivoTrue(99, 12))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicioService.eliminarServicio(7, 99))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                BusinessRuleException businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode()).isEqualTo("SERVICE_NOT_FOUND");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });

        verify(servicioRepository, never()).save(any(Servicio.class));
        verify(reservaRepository, never()).existsByServicio_IdAndEstadoIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("Eliminar sin perfil de proveedor responde 404 sin consultar servicios")
    void eliminarServicioSinPerfilProveedorResponde404() {
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of());

        assertThatThrownBy(() -> servicioService.eliminarServicio(7, 41))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                BusinessRuleException businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode()).isEqualTo("PROVIDER_PROFILE_NOT_FOUND");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    @DisplayName("Eliminar con perfiles duplicados responde 409 sin elegir uno")
    void eliminarServicioConPerfilesDuplicadosResponde409() {
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(
            List.of(Proveedor.builder().id(12).build(), Proveedor.builder().id(13).build())
        );

        assertThatThrownBy(() -> servicioService.eliminarServicio(7, 41))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                BusinessRuleException businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode())
                    .isEqualTo("PROVIDER_PROFILE_AMBIGUOUS");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

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
        assertThat(response).isEqualTo(
            new ServicioResponse(41, "Corte", "Corte de cabello", 30)
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
        CrearServicioRequest request = new CrearServicioRequest("Corte", "Descripción", 30);

        servicioService.crearServicio(7, request);
        servicioService.crearServicio(7, request);

        verify(servicioRepository, times(2)).save(any(Servicio.class));
    }
}
