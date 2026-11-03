package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.proveedores.repository.IProveedorRepository;
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

        Servicio servicio = Servicio.builder()
            .proveedor(perfiles.getFirst())
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
}
