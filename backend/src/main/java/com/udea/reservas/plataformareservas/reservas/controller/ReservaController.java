package com.udea.reservas.plataformareservas.reservas.controller;

import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.reservas.dto.*;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Requiere sesión iniciada (ver ApiSecurityConfiguration): el usuario de cada
 * operación es el de la sesión, no un dato enviado por el cliente.
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponse> crearReserva(
            @AuthenticationPrincipal UserPrincipal usuario,
            @Valid @RequestBody CrearReservaRequest request) {
        Reserva reserva = reservaService.crearReserva(
                usuario.getId(),
                request.servicioId(),
                request.fecha(),
                request.hora()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponse.fromEntity(reserva));
    }

    @DeleteMapping("/{reservaId}/cancelar")
    public ResponseEntity<ReservaCanceladaResponse> cancelarReserva(
            @AuthenticationPrincipal UserPrincipal usuario,
            @PathVariable Integer reservaId) {
        Reserva reserva = reservaService.cancelarReserva(reservaId, usuario.getId());
        return ResponseEntity.ok(new ReservaCanceladaResponse(
                reserva.getId(),
                reserva.getEstado(),
                "Reserva cancelada correctamente. El horario queda disponible para otros usuarios."
        ));
    }

    @ExceptionHandler(ReservaException.class)
    public ResponseEntity<String> manejarReservaException(ReservaException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    @GetMapping("/historial")
    public ResponseEntity<List<ReservaHistorialResponse>> consultarHistorial(
            @AuthenticationPrincipal UserPrincipal usuario) {
        List<ReservaHistorialResponse> historial = reservaService.obtenerHistorialUsuario(usuario.getId());
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/{reservaId}/detalle")
    public ResponseEntity<ReservaDetalleResponse> consultarDetalleReserva(
            @AuthenticationPrincipal UserPrincipal usuario,
            @PathVariable Integer reservaId) {
        ReservaDetalleResponse detalle = reservaService.obtenerDetalleReserva(reservaId, usuario.getId());
        return ResponseEntity.ok(detalle);
    }
}
