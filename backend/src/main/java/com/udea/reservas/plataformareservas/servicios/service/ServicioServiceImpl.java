package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
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

    private static final String ESTADO_ACTIVA = "activa";

    private final IProveedorRepository proveedorRepository;
    private final IServicioRepository servicioRepository;
    private final IReservaRepository reservaRepository;

    @Override
    @Transactional
    public ServicioResponse crearServicio(Integer usuarioId, CrearServicioRequest request) {
        Proveedor proveedor = resolverProveedor(usuarioId, "no se creó el servicio");

        Servicio servicio = Servicio.builder()
            .proveedor(proveedor)
            .nombre(request.nombre())
            .descripcion(request.descripcion())
            .duracionMinutos(request.duracion())
            .build();
        Servicio guardado = servicioRepository.save(servicio);

        return new ServicioResponse(
            guardado.getId(),
            guardado.getNombre(),
            guardado.getDescripcion(),
            guardado.getDuracionMinutos()
        );
    }

    @Override
    @Transactional
    public void eliminarServicio(Integer usuarioId, Integer servicioId) {
        Proveedor proveedor = resolverProveedor(usuarioId, "no se eliminó el servicio");

        // 404 idéntico para inexistente, ajeno o ya eliminado: no revela su existencia
        Servicio servicio = servicioRepository
            .findByIdAndProveedor_IdAndActivoTrue(servicioId, proveedor.getId())
            .orElseThrow(() -> new BusinessRuleException(
                "SERVICE_NOT_FOUND",
                "El servicio no existe.",
                HttpStatus.NOT_FOUND.value()
            ));

        if (reservaRepository.existsByServicio_IdAndEstadoIgnoreCase(servicioId, ESTADO_ACTIVA)) {
            throw new BusinessRuleException(
                "SERVICE_HAS_ACTIVE_RESERVATIONS",
                "No se puede eliminar un servicio con reservas activas.",
                HttpStatus.CONFLICT.value()
            );
        }

        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }

    private Proveedor resolverProveedor(Integer usuarioId, String accion) {
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
                "El usuario tiene más de un perfil de proveedor; " + accion + ".",
                HttpStatus.CONFLICT.value()
            );
        }
        return perfiles.getFirst();
    }
}
