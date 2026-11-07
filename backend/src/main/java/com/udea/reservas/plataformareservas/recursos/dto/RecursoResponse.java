package com.udea.reservas.plataformareservas.recursos.dto;

import com.udea.reservas.plataformareservas.recursos.model.Recurso;

public record RecursoResponse(
    Integer id,
    String nombre,
    String tipo,
    boolean activo
) {

    public static RecursoResponse fromEntity(Recurso recurso) {
        return new RecursoResponse(
            recurso.getId(),
            recurso.getNombre(),
            recurso.getTipo(),
            recurso.isActivo()
        );
    }
}
