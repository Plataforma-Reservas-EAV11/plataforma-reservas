package com.udea.reservas.plataformareservas.servicios.dto;

public record ServicioResponse(
    Integer id,
    String nombre,
    String descripcion,
    Integer duracion
) {}
