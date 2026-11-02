package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;

public interface ServicioService {

    ServicioResponse crearServicio(Integer usuarioId, CrearServicioRequest request);
}
