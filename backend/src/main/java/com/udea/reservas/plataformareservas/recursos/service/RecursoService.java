package com.udea.reservas.plataformareservas.recursos.service;

import com.udea.reservas.plataformareservas.recursos.dto.GuardarRecursoRequest;
import com.udea.reservas.plataformareservas.recursos.dto.RecursoResponse;
import java.util.List;

public interface RecursoService {

    RecursoResponse crearRecurso(Integer usuarioId, GuardarRecursoRequest request);

    List<RecursoResponse> listarRecursos(Integer usuarioId);

    RecursoResponse consultarRecurso(Integer usuarioId, Integer recursoId);

    RecursoResponse actualizarRecurso(
        Integer usuarioId,
        Integer recursoId,
        GuardarRecursoRequest request
    );

    RecursoResponse cambiarEstado(
        Integer usuarioId,
        Integer recursoId,
        boolean activo
    );

    void asociarServicio(Integer usuarioId, Integer recursoId, Integer servicioId);

    void retirarServicio(Integer usuarioId, Integer recursoId, Integer servicioId);
}
