package com.udea.reservas.plataformareservas.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecursoUsoDTO {
    private String nombreRecurso;
    private Long cantidadUso;
}