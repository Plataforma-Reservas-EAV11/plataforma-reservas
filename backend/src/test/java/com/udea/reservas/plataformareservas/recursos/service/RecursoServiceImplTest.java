package com.udea.reservas.plataformareservas.recursos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
import com.udea.reservas.plataformareservas.recursos.dto.GuardarRecursoRequest;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.recursos.repository.RecursoRepository;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RecursoServiceImplTest {

    @Mock
    private IProveedorRepository proveedorRepository;

    @Mock
    private RecursoRepository recursoRepository;

    @Mock
    private IServicioRepository servicioRepository;

    @InjectMocks
    private RecursoServiceImpl recursoService;

    private Proveedor proveedor;

    @BeforeEach
    void setUp() {
        proveedor = Proveedor.builder().id(12).build();
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(List.of(proveedor));
    }

    @Test
    @DisplayName("Crea un recurso activo asociado al proveedor autenticado")
    void crearRecursoAsociaProveedorDeLaSesion() {
        when(recursoRepository.save(any(Recurso.class))).thenAnswer(invocation -> {
            Recurso recurso = invocation.getArgument(0);
            recurso.setId(41);
            return recurso;
        });

        var response = recursoService.crearRecurso(
            7,
            new GuardarRecursoRequest(" Consultorio ", " sala ")
        );

        assertThat(response.id()).isEqualTo(41);
        assertThat(response.nombre()).isEqualTo("Consultorio");
        assertThat(response.tipo()).isEqualTo("sala");
        assertThat(response.activo()).isTrue();
        org.mockito.ArgumentCaptor<Recurso> captor = org.mockito.ArgumentCaptor.forClass(Recurso.class);
        verify(recursoRepository).save(captor.capture());
        assertThat(captor.getValue().getProveedor()).isSameAs(proveedor);
    }

    @Test
    @DisplayName("Lista únicamente los recursos asociados al perfil del proveedor")
    void listarRecursosFiltraPorProveedorAutenticado() {
        Recurso recurso = recurso(41, true);
        when(recursoRepository.findAllByProveedor_IdOrderByIdAsc(12))
            .thenReturn(List.of(recurso));

        var recursos = recursoService.listarRecursos(7);

        assertThat(recursos).singleElement()
            .satisfies(response -> assertThat(response.id()).isEqualTo(41));
        verify(recursoRepository).findAllByProveedor_IdOrderByIdAsc(12);
    }

    @Test
    @DisplayName("No permite consultar un recurso de otro proveedor")
    void consultarRecursoAjenoRespondeNoEncontrado() {
        when(recursoRepository.findByIdAndProveedor_Id(99, 12)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recursoService.consultarRecurso(7, 99))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                var businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });
    }

    @Test
    @DisplayName("Actualiza únicamente el recurso propio")
    void actualizarRecursoPropio() {
        Recurso recurso = recurso(41, true);
        when(recursoRepository.findOwnedForUpdate(41, 12)).thenReturn(Optional.of(recurso));
        when(recursoRepository.save(recurso)).thenReturn(recurso);

        var response = recursoService.actualizarRecurso(
            7,
            41,
            new GuardarRecursoRequest("Sala 2", null)
        );

        assertThat(response.nombre()).isEqualTo("Sala 2");
        assertThat(response.tipo()).isNull();
        assertThat(recurso.isActivo()).isTrue();
    }

    @Test
    @DisplayName("Desactivar un recurso no lo elimina ni lo altera en reservas históricas")
    void desactivarRecursoEsLogicoYConservaHistorial() {
        Recurso recurso = recurso(41, true);
        when(recursoRepository.findOwnedForUpdate(41, 12)).thenReturn(Optional.of(recurso));
        when(recursoRepository.save(recurso)).thenReturn(recurso);

        var response = recursoService.cambiarEstado(7, 41, false);

        assertThat(response.activo()).isFalse();
        verify(recursoRepository).save(recurso);
        verify(recursoRepository, never()).delete(any(Recurso.class));
    }

    @Test
    @DisplayName("Permite reactivar un recurso propio")
    void activarRecursoPropio() {
        Recurso recurso = recurso(41, false);
        when(recursoRepository.findOwnedForUpdate(41, 12)).thenReturn(Optional.of(recurso));
        when(recursoRepository.save(recurso)).thenReturn(recurso);

        assertThat(recursoService.cambiarEstado(7, 41, true).activo()).isTrue();
    }

    @Test
    @DisplayName("Asocia un recurso propio al servicio propio de forma idempotente")
    void asociarRecursoServicioPropio() {
        Recurso recurso = recurso(41, true);
        Servicio servicio = Servicio.builder()
            .id(51)
            .proveedor(proveedor)
            .recursosCompatibles(new ArrayList<>())
            .build();
        when(recursoRepository.findOwnedForUpdate(41, 12)).thenReturn(Optional.of(recurso));
        when(servicioRepository.findByIdAndProveedor_Id(51, 12))
            .thenReturn(Optional.of(servicio));
        when(servicioRepository.save(servicio)).thenReturn(servicio);

        recursoService.asociarServicio(7, 41, 51);
        recursoService.asociarServicio(7, 41, 51);

        assertThat(servicio.getRecursosCompatibles()).containsExactly(recurso);
        verify(servicioRepository).save(servicio);
    }

    @Test
    @DisplayName("Rechaza asociar un recurso a un servicio de otro proveedor")
    void asociarRecursoAServicioAjenoDebeFallar() {
        when(recursoRepository.findOwnedForUpdate(41, 12))
            .thenReturn(Optional.of(recurso(41, true)));
        when(servicioRepository.findByIdAndProveedor_Id(99, 12)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recursoService.asociarServicio(7, 41, 99))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                var businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode()).isEqualTo("SERVICE_NOT_FOUND");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            });

        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    @DisplayName("Retira la asociación sin eliminar el recurso ni el servicio")
    void retirarAsociacionConservaEntidades() {
        Recurso recurso = recurso(41, false);
        Servicio servicio = Servicio.builder()
            .id(51)
            .proveedor(proveedor)
            .recursosCompatibles(new ArrayList<>(List.of(recurso)))
            .build();
        when(recursoRepository.findOwnedForUpdate(41, 12)).thenReturn(Optional.of(recurso));
        when(servicioRepository.findByIdAndProveedor_Id(51, 12))
            .thenReturn(Optional.of(servicio));

        recursoService.retirarServicio(7, 41, 51);

        assertThat(servicio.getRecursosCompatibles()).isEmpty();
        verify(recursoRepository, never()).delete(any(Recurso.class));
        verify(servicioRepository, never()).delete(any(Servicio.class));
    }

    @Test
    @DisplayName("Rechaza la operación cuando el usuario tiene más de un perfil proveedor")
    void perfilProveedorAmbiguoNoEligeArbitrariamente() {
        when(proveedorRepository.findAllByUsuario_Id(7)).thenReturn(
            List.of(proveedor, Proveedor.builder().id(13).build())
        );

        assertThatThrownBy(() -> recursoService.listarRecursos(7))
            .isInstanceOf(BusinessRuleException.class)
            .satisfies(exception -> {
                var businessException = (BusinessRuleException) exception;
                assertThat(businessException.getErrorCode()).isEqualTo("PROVIDER_PROFILE_AMBIGUOUS");
                assertThat(businessException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });
    }

    private Recurso recurso(int id, boolean activo) {
        return Recurso.builder()
            .id(id)
            .proveedor(proveedor)
            .nombre("Recurso " + id)
            .tipo("sala")
            .activo(activo)
            .build();
    }
}
