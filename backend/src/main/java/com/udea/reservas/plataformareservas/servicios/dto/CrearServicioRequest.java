package com.udea.reservas.plataformareservas.servicios.dto;

import com.udea.reservas.plataformareservas.servicios.dto.validation.StrictIntegerDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.annotation.JsonDeserialize;

public record CrearServicioRequest(
    @NotBlank(message = "El nombre es obligatorio y no puede estar vacío.")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres.")
    String nombre,

    @NotBlank(message = "La descripción es obligatoria y no puede estar vacía.")
    @Size(max = 500, message = "La descripción no puede superar 500 caracteres.")
    String descripcion,

    @NotNull(message = "La duración en minutos es obligatoria.")
    @Positive(message = "La duración en minutos debe ser un entero positivo.")
    @JsonDeserialize(using = StrictIntegerDeserializer.class)
    Integer duracion
) {}
