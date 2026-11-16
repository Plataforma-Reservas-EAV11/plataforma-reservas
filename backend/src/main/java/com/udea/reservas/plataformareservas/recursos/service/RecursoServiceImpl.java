package com.udea.reservas.plataformareservas.recursos.service;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
import com.udea.reservas.plataformareservas.recursos.dto.GuardarRecursoRequest;
import com.udea.reservas.plataformareservas.recursos.dto.RecursoResponse;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.recursos.repository.RecursoRepository;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecursoServiceImpl implements RecursoService {

    private final IProveedorRepository proveedorRepository;
    private final RecursoRepository recursoRepository;
    private final IServicioRepository servicioRepository;

    @Override
    @Transactional
    public RecursoResponse crearRecurso(Integer usuarioId, GuardarRecursoRequest request) {
        Proveedor proveedor = obtenerProveedor(usuarioId);
        Recurso recurso = Recurso.builder()
            .proveedor(proveedor)
            .nombre(request.nombre().trim())
            .tipo(normalizarTipo(request.tipo()))
            .activo(true)
            .build();
        return RecursoResponse.fromEntity(recursoRepository.save(recurso));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecursoResponse> listarRecursos(Integer usuarioId) {
        Proveedor proveedor = obtenerProveedor(usuarioId);
        return recursoRepository.findAllByProveedor_IdOrderByIdAsc(proveedor.getId())
            .stream()
            .map(RecursoResponse::fromEntity)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecursoResponse consultarRecurso(Integer usuarioId, Integer recursoId) {
        Proveedor proveedor = obtenerProveedor(usuarioId);
        return recursoRepository.findByIdAndProveedor_Id(recursoId, proveedor.getId())
            .map(RecursoResponse::fromEntity)
            .orElseThrow(() -> recursoNoExiste());
    }

    @Override
    @Transactional
    public RecursoResponse actualizarRecurso(
        Integer usuarioId,
        Integer recursoId,
        GuardarRecursoRequest request
    ) {
        Recurso recurso = obtenerRecursoParaActualizar(usuarioId, recursoId);
        recurso.setNombre(request.nombre().trim());
        recurso.setTipo(normalizarTipo(request.tipo()));
        return RecursoResponse.fromEntity(recursoRepository.save(recurso));
    }

    @Override
    @Transactional
    public RecursoResponse cambiarEstado(Integer usuarioId, Integer recursoId, boolean activo) {
        Recurso recurso = obtenerRecursoParaActualizar(usuarioId, recursoId);
        recurso.setActivo(activo);
        return RecursoResponse.fromEntity(recursoRepository.save(recurso));
    }

    @Override
    @Transactional
    public void asociarServicio(Integer usuarioId, Integer recursoId, Integer servicioId) {
        Proveedor proveedor = obtenerProveedor(usuarioId);
        Recurso recurso = recursoRepository.findOwnedForUpdate(recursoId, proveedor.getId())
            .orElseThrow(() -> recursoNoExiste());
        Servicio servicio = servicioRepository.findByIdAndProveedor_Id(servicioId, proveedor.getId())
            .orElseThrow(() -> servicioNoExiste());

        boolean yaAsociado = servicio.getRecursosCompatibles().stream()
            .anyMatch(asociado -> asociado.getId().equals(recurso.getId()));
        if (!yaAsociado) {
            servicio.getRecursosCompatibles().add(recurso);
            servicioRepository.save(servicio);
        }
    }

    @Override
    @Transactional
    public void retirarServicio(Integer usuarioId, Integer recursoId, Integer servicioId) {
        Proveedor proveedor = obtenerProveedor(usuarioId);
        Recurso recurso = recursoRepository.findOwnedForUpdate(recursoId, proveedor.getId())
            .orElseThrow(() -> recursoNoExiste());
        Servicio servicio = servicioRepository.findByIdAndProveedor_Id(servicioId, proveedor.getId())
            .orElseThrow(() -> servicioNoExiste());
        boolean removed = servicio.getRecursosCompatibles()
            .removeIf(asociado -> asociado.getId().equals(recurso.getId()));
        if (removed) {
            servicioRepository.save(servicio);
        }
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

    private Recurso obtenerRecursoParaActualizar(Integer usuarioId, Integer recursoId) {
        Proveedor proveedor = obtenerProveedor(usuarioId);
        return recursoRepository.findOwnedForUpdate(recursoId, proveedor.getId())
            .orElseThrow(() -> recursoNoExiste());
    }

    private static String normalizarTipo(String tipo) {
        return tipo == null || tipo.isBlank() ? null : tipo.trim();
    }

    private static BusinessRuleException recursoNoExiste() {
        return new BusinessRuleException(
            "RESOURCE_NOT_FOUND",
            "El recurso no existe.",
            HttpStatus.NOT_FOUND.value()
        );
    }

    private static BusinessRuleException servicioNoExiste() {
        return new BusinessRuleException(
            "SERVICE_NOT_FOUND",
            "El servicio no existe para el proveedor autenticado.",
            HttpStatus.NOT_FOUND.value()
        );
    }
}
