package com.udea.reservas.plataformareservas.agendas.controller;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.exception.AgendaException;
import com.udea.reservas.plataformareservas.agendas.service.HorarioDisponibleService;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Requiere sesión iniciada (ver ApiSecurityConfiguration). El servicio verifica
 * además que la agenda pertenezca al proveedor autenticado.
 */
@RestController
@RequestMapping("/api/agendas")
@RequiredArgsConstructor
public class HorarioDisponibleController {

    private final HorarioDisponibleService service;

    @PostMapping("/{agendaId}/horarios")
    public ResponseEntity<Map<String, Object>> agregarHorario(
            @AuthenticationPrincipal UserPrincipal usuario,
            @PathVariable Integer agendaId,
            @Valid @RequestBody CrearHorarioRequest request) {

        HorarioResponse horario = service.crearHorario(agendaId, usuario.getId(), request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mensaje", "Bloque de disponibilidad creado exitosamente.");
        response.put("data", horario);

        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(AgendaException.class)
    public ResponseEntity<String> manejarAgendaException(AgendaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }
}
