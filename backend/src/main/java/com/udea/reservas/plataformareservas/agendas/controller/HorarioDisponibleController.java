package com.udea.reservas.plataformareservas.agendas.controller;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.service.HorarioDisponibleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agendas")
public class HorarioDisponibleController {

    @Autowired
    private HorarioDisponibleService service;

    @PostMapping("/{agendaId}/horarios")
    public ResponseEntity<HorarioResponse> agregarHorario(
            @PathVariable Integer agendaId,
            @RequestBody CrearHorarioRequest request) {

        HorarioResponse response = service.crearHorario(agendaId, request);
        return ResponseEntity.ok(response);
    }
}