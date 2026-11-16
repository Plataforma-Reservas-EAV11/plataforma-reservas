package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResumenResponse;
import java.util.List;

public interface ServicioService {

    ServicioResponse crearServicio(Integer usuarioId, CrearServicioRequest request);

    void eliminarServicio(Integer usuarioId, Integer servicioId);

    /** Servicios vigentes del proveedor autenticado, ordenados por nombre. */
    List<ServicioResumenResponse> listarServicios(Integer usuarioId);
}
