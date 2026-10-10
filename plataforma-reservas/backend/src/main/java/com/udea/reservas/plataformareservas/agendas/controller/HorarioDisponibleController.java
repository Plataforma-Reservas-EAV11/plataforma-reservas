package com.udea.reservas.plataformareservas.agendas.controller;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.service.HorarioDisponibleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agendas")
@RequiredArgsConstructor
public class HorarioDisponibleController {

    private final HorarioDisponibleService service;

    @PostMapping("/{agendaId}/horarios")
    public ResponseEntity<HorarioResponse> agregarHorario(
            @PathVariable Integer agendaId,
            @RequestBody CrearHorarioRequest request) {

        HorarioResponse response = service.crearHorario(agendaId, request);
        return ResponseEntity.ok(response);
    }
}