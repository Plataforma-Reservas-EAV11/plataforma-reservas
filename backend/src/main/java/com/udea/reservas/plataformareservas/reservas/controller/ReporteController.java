package com.udea.reservas.plataformareservas.reservas.controller;

import com.udea.reservas.plataformareservas.reservas.dto.ReporteOcupacionDTO;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReservaService reservaService;

    @GetMapping("/ocupacion")
    public ResponseEntity<?> obtenerReporteOcupacion(
            @RequestParam("fechaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fechaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        try {
            ReporteOcupacionDTO reporte = reservaService.obtenerReporteOcupacion(fechaInicio, fechaFin);
            return ResponseEntity.ok(reporte);
        } catch (IllegalArgumentException e) {
            // Manejamos la excepción que lanzaste en el servicio cuando la fecha de inicio es mayor
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}