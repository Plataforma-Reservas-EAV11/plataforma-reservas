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
