package com.udea.reservas.plataformareservas.agendas.service;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;

public interface HorarioDisponibleService {

    /**
     * Agrega un bloque de disponibilidad a la agenda. {@code usuarioId} es el
     * usuario autenticado: debe ser el dueño del proveedor de la agenda.
     */
    HorarioResponse crearHorario(
        Integer agendaId,
        Integer usuarioId,
        CrearHorarioRequest request
    );
}
