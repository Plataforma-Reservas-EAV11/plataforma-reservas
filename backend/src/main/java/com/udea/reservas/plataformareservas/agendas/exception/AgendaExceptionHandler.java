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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("El método HTTP '" + ex.getMethod() + "' no está soportado para este recurso.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> manejarRecursoNoEncontrado(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("El recurso solicitado no existe: " + ex.getResourcePath());
    }

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
        String mensajeError = obtenerMensajeCausaRaiz(ex);

        if (esErrorDeSolapamiento(mensajeError)) {
            return ResponseEntity.badRequest().body("El horario ya tiene disponibilidad definida. No se pueden solapar bloques.");
        }

        if (esErrorDeRangoDeHoras(mensajeError)) {
            return ResponseEntity.badRequest().body("El rango de horas no es válido. La hora de fin debe ser posterior a la de inicio.");
        }

        return ResponseEntity.internalServerError().body("Error interno del servidor: " + ex.getMessage());
    }

    // ========================================================================
    // MÉTODOS PRIVADOS AUXILIARES (REDUCEN LA COMPLEJIDAD CICLOMÁTICA)
    // ========================================================================

    private String obtenerMensajeCausaRaiz(Throwable ex) {
        Throwable causa = ex;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        return causa.getMessage();
    }

    private boolean esErrorDeSolapamiento(String mensajeError) {
        return mensajeError != null && mensajeError.toLowerCase().contains("solapa");
    }

    private boolean esErrorDeRangoDeHoras(String mensajeError) {
        return mensajeError != null && (mensajeError.contains("chk_hora_valida") || mensajeError.contains("hora_fin"));
    }
}