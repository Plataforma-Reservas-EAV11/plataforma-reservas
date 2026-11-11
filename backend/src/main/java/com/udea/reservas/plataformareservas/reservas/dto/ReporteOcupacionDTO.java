package com.udea.reservas.plataformareservas.reservas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteOcupacionDTO {
    private Long totalReservas;
    private List<ServicioOcupacionDTO> ocupacionPorServicio;
    private List<RecursoUsoDTO> usoPorRecurso;
}