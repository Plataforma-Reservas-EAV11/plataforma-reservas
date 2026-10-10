package com.udea.reservas.plataformareservas.agendas.controller;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.service.HorarioDisponibleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agendas")
@RequiredArgsConstructor
public class HorarioDisponibleController {

    private final HorarioDisponibleService service;

    @PostMapping("/{agendaId}/horarios")
    public ResponseEntity<Map<String, Object>> agregarHorario(
            @PathVariable Integer agendaId,
            @RequestBody CrearHorarioRequest request) {

        HorarioResponse horario = service.crearHorario(agendaId, request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Bloque de disponibilidad creado exitosamente.");
        response.put("data", horario);

        return ResponseEntity.ok(response);
    }
}