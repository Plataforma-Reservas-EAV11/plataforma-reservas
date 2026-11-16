package com.udea.reservas.plataformareservas.recursos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardarRecursoRequest(
    @NotBlank(message = "El nombre es obligatorio y no puede estar vacío.")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres.")
    String nombre,

    @Size(max = 50, message = "El tipo no puede superar 50 caracteres.")
    String tipo
) {}
