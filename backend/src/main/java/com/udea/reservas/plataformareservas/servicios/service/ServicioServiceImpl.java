package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.recursos.repository.RecursoRepository;
import com.udea.reservas.plataformareservas.reservas.repository.IReservaRepository;
import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServicioServiceImpl implements ServicioService {

    private final IProveedorRepository proveedorRepository;
    private final IServicioRepository servicioRepository;
    private final IAgendaRepository agendaRepository;
    private final RecursoRepository recursoRepository;
    private final IReservaRepository reservaRepository;

    @Override
    @Transactional
    public ServicioResponse crearServicio(Integer usuarioId, CrearServicioRequest request) {
        Proveedor proveedor = obtenerProveedor(usuarioId);
        List<Recurso> recursos = resolverRecursos(proveedor, request.recursoIds());

        Servicio servicio = Servicio.builder()
            .proveedor(proveedor)
            .nombre(request.nombre())
            .descripcion(request.descripcion())
            .duracionMinutos(request.duracion())
            .recursosCompatibles(recursos)
            .build();
        Servicio guardado = servicioRepository.save(servicio);
        Agenda agenda = agendaRepository.save(
            Agenda.builder()
                .proveedor(proveedor)
                .servicio(guardado)
                .activa(true)
                .build()
        );

        List<Integer> idsRecursos = recursos.stream()
            .map(Recurso::getId)
            .toList();

        return new ServicioResponse(
            guardado.getId(),
            agenda.getId(),
            guardado.getNombre(),
            guardado.getDescripcion(),
            guardado.getDuracionMinutos(),
            idsRecursos
        );
    }

    /**
     * Resuelve y valida los recursos que se asociarán al servicio.
     * Un servicio sin recursos activos no genera turnos reservables, por lo que
     * el alta exige al menos un recurso del proveedor autenticado y activo.
     */
    private List<Recurso> resolverRecursos(
        Proveedor proveedor,
        List<Integer> recursoIdsSolicitados
    ) {
        List<Integer> recursoIds = recursoIdsSolicitados == null
            ? List.of()
            : recursoIdsSolicitados;

        if (recursoIds.isEmpty()) {
            throw new BusinessRuleException(
                "RESOURCE_REQUIRED",
                "Debes asociar al menos un recurso activo al servicio.",
                HttpStatus.BAD_REQUEST.value()
            );
        }
        if (recursoIds.stream().distinct().count() != recursoIds.size()) {
            throw new BusinessRuleException(
                "INVALID_SERVICE_RESOURCES",
                "La lista de recursos contiene identificadores duplicados.",
                HttpStatus.BAD_REQUEST.value()
            );
        }

        List<Recurso> recursos =
            recursoRepository.findAllByIdInAndProveedor_Id(recursoIds, proveedor.getId());

        if (recursos.size() != recursoIds.size()) {
            throw new BusinessRuleException(
                "INVALID_SERVICE_RESOURCES",
                "Todos los recursos deben existir y pertenecer al proveedor autenticado.",
                HttpStatus.BAD_REQUEST.value()
            );
        }
        if (recursos.stream().noneMatch(Recurso::isActivo)) {
            throw new BusinessRuleException(
                "INVALID_SERVICE_RESOURCES",
                "El servicio necesita al menos un recurso activo.",
                HttpStatus.BAD_REQUEST.value()
            );
        }

        return recursos;
    }

    @Override
    @Transactional
    public void eliminarServicio(Integer usuarioId, Integer servicioId) {
        Proveedor proveedor = obtenerProveedor(usuarioId);
        Servicio servicio = servicioRepository.findActivoForUpdate(servicioId)
            .filter(encontrado -> encontrado.getProveedor().getId().equals(proveedor.getId()))
            .orElseThrow(() -> new BusinessRuleException(
                "SERVICE_NOT_FOUND",
                "El servicio no existe para el proveedor autenticado.",
                HttpStatus.NOT_FOUND.value()
            ));
        if (reservaRepository.existsByServicio_IdAndEstadoIgnoreCase(servicioId, "activa")) {
            throw new BusinessRuleException(
                "SERVICE_HAS_ACTIVE_RESERVATIONS",
                "El servicio tiene reservas activas.",
                HttpStatus.CONFLICT.value()
            );
        }
        servicio.setEliminado(true);
        servicioRepository.save(servicio);
    }

    private Proveedor obtenerProveedor(Integer usuarioId) {
        List<Proveedor> perfiles = proveedorRepository.findAllByUsuario_Id(usuarioId);
        if (perfiles.isEmpty()) {
            throw new BusinessRuleException(
                "PROVIDER_PROFILE_NOT_FOUND",
                "No existe un perfil de proveedor para el usuario autenticado.",
                HttpStatus.NOT_FOUND.value()
            );
        }
        if (perfiles.size() > 1) {
            throw new BusinessRuleException(
                "PROVIDER_PROFILE_AMBIGUOUS",
                "El usuario tiene más de un perfil de proveedor.",
                HttpStatus.CONFLICT.value()
            );
        }
        return perfiles.getFirst();
    }
}
