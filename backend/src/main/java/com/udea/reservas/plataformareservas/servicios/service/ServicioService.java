package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;

public interface ServicioService {

    ServicioResponse crearServicio(Integer usuarioId, CrearServicioRequest request);

    /** Eliminación lógica del servicio activo del proveedor autenticado, si no tiene reservas activas. */
    void eliminarServicio(Integer usuarioId, Integer servicioId);
}
