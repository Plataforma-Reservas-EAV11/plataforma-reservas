package com.udea.reservas.plataformareservas.recursos.dto;

import jakarta.validation.constraints.NotNull;

public record EstadoRecursoRequest(
    @NotNull(message = "El estado activo es obligatorio.")
    Boolean activo
) {}
