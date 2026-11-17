package com.udea.reservas.plataformareservas.servicios.dto;

import com.udea.reservas.plataformareservas.servicios.model.Servicio;

/**
 * Fila del listado "Mis servicios" del proveedor. El modelo no guarda un estado
 * propio del servicio: los eliminados no se listan, así que cualquier servicio que
 * aparece aquí está "activo".
 */
public record ServicioResumenResponse(
    Integer id,
    String nombre,
    Integer duracionMinutos,
    String estado
) {

    public static final String ESTADO_ACTIVO = "activo";

    public static ServicioResumenResponse fromEntity(Servicio servicio) {
        return new ServicioResumenResponse(
            servicio.getId(),
            servicio.getNombre(),
            servicio.getDuracionMinutos(),
            ESTADO_ACTIVO
        );
    }
}
