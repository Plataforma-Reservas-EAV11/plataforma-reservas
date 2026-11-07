package com.udea.reservas.plataformareservas.servicios.dto;

import java.util.List;

public record ServicioResponse(
    Integer id,
    Integer agendaId,
    String nombre,
    String descripcion,
    Integer duracion,
    List<Integer> recursoIds
) {

    public ServicioResponse(
        Integer id,
        Integer agendaId,
        String nombre,
        String descripcion,
        Integer duracion
    ) {
        this(id, agendaId, nombre, descripcion, duracion, List.of());
    }
}
