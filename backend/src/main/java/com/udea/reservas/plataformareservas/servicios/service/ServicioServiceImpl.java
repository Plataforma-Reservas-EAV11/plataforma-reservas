package com.udea.reservas.plataformareservas.servicios.service;

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
import java.util.ArrayList;
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

    @Override
    @Transactional
    public ServicioResponse crearServicio(Integer usuarioId, CrearServicioRequest request) {
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
                "El usuario tiene más de un perfil de proveedor; no se creó el servicio.",
                HttpStatus.CONFLICT.value()
            );
        }

        Proveedor proveedor = perfiles.getFirst();
        List<Integer> recursoIds = request.recursoIds() == null
            ? List.of()
            : request.recursoIds();
        if (recursoIds.stream().distinct().count() != recursoIds.size()) {
            throw new BusinessRuleException(
                "INVALID_SERVICE_RESOURCES",
                "La lista de recursos contiene identificadores duplicados.",
                HttpStatus.BAD_REQUEST.value()
            );
        }
        List<Recurso> recursos = recursoIds.isEmpty()
            ? new ArrayList<>()
            : recursoRepository.findAllByIdInAndProveedor_Id(recursoIds, proveedor.getId());
        if (recursos.size() != recursoIds.size()) {
            throw new BusinessRuleException(
                "INVALID_SERVICE_RESOURCES",
                "Todos los recursos deben existir y pertenecer al proveedor autenticado.",
                HttpStatus.BAD_REQUEST.value()
            );
        }

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

        List<Integer> idsRecursos = new ArrayList<>(recursos.size());
        for (Recurso recurso : recursos) {
            idsRecursos.add(recurso.getId());
        }

        return new ServicioResponse(
            guardado.getId(),
            agenda.getId(),
            guardado.getNombre(),
            guardado.getDescripcion(),
            guardado.getDuracionMinutos(),
            idsRecursos
        );
    }
}
