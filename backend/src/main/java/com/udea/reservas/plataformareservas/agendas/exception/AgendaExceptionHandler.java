package com.udea.reservas.plataformareservas.agendas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class AgendaExceptionHandler {

    @ExceptionHandler(AgendaException.class)
    public ResponseEntity<String> manejarAgendaException(AgendaException ex) {
        return ResponseEntity.status(404).body(ex.getMessage());
    }

    // Devuelve HTTP 405 en lugar de 500 cuando el método HTTP no está soportado
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("El método HTTP '" + ex.getMethod() + "' no está soportado para este recurso.");
    }

    // Devuelve HTTP 404 en lugar de 500 cuando el recurso o endpoint no existe
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> manejarRecursoNoEncontrado(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("El recurso solicitado no existe: " + ex.getResourcePath());
    }

    // Manejo de validaciones @Valid para la solución del Bug 38
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
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