package com.udea.reservas.plataformareservas.servicios.dto;

public record ProviderServiceResponse(
    Integer id,
    String nombre,
    Integer duracionMinutos,
    String proveedorNombre
) {}
