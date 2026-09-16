package com.udea.reservas.plataformareservas.reservas.dto;

public record ReservaCanceladaResponse(
        Integer reservaId,
        String estado,
        String mensaje
) {
}
