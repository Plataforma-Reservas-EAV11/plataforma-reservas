package com.udea.reservas.plataformareservas.agendas.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AgendaExceptionHandler {

    @ExceptionHandler(AgendaException.class)
    public ResponseEntity<String> manejarAgendaException(AgendaException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> manejarErroresGlobales(Exception ex) {
        Throwable causa = ex;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }

        String mensajeError = causa.getMessage();

        if (mensajeError != null && mensajeError.toLowerCase().contains("solapa")) {
            return ResponseEntity.badRequest().body("El horario ya tiene disponibilidad definida. No se pueden solapar bloques.");
        }
        if (mensajeError != null && (mensajeError.contains("chk_hora_valida") || mensajeError.contains("hora_fin"))) {
            return ResponseEntity.badRequest().body("El rango de horas no es válido. La hora de fin debe ser posterior a la de inicio.");
        }

        return ResponseEntity.internalServerError().body("Error interno del servidor: " + ex.getMessage());
    }
}