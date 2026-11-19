package com.udea.reservas.plataformareservas.reservas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    public ReporteOcupacionDTO(
        Long totalReservas,
        List<ServicioOcupacionDTO> ocupacionPorServicio,
        List<RecursoUsoDTO> usoPorRecurso
    ) {
        this.totalReservas = totalReservas;
        this.ocupacionPorServicio = ocupacionPorServicio;
        this.usoPorRecurso = usoPorRecurso;
    }
}