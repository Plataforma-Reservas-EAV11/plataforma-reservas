package com.udea.reservas.plataformareservas.reservas.controller;

import com.udea.reservas.plataformareservas.reservas.dto.CrearReservaRequest;
import com.udea.reservas.plataformareservas.reservas.dto.ReservaResponse;
import com.udea.reservas.plataformareservas.reservas.dto.ReservaCanceladaResponse;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    public ResponseEntity<ReservaResponse> crearReserva(@Valid @RequestBody CrearReservaRequest request) {
        Reserva reserva = reservaService.crearReserva(
                request.usuarioId(),
                request.horarioId(),
                request.fecha(),
                request.hora()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponse.fromEntity(reserva));
    }

    @DeleteMapping("/{reservaId}/cancelar")
    public ResponseEntity<ReservaCanceladaResponse> cancelarReserva(
            @PathVariable Integer reservaId,
            @RequestParam Integer usuarioId) {
        Reserva reserva = reservaService.cancelarReserva(reservaId, usuarioId);
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
}