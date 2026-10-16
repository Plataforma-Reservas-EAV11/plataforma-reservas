package com.udea.reservas.plataformareservas.common.exception;

import com.udea.reservas.plataformareservas.auth.exception.EmailAlreadyRegisteredException;
import com.udea.reservas.plataformareservas.auth.exception.PasswordMismatchException;
import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import com.udea.reservas.plataformareservas.common.dto.ApiResponseDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto> handleValidationException(
        MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception
            .getBindingResult()
            .getFieldErrors()
            .forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
            );

        return ResponseEntity.badRequest().body(
            ApiResponseDto.validationFailure(
                "Hay campos obligatorios o inválidos.",
                errors
            )
        );
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiResponseDto> handleEmailAlreadyRegistered(
        EmailAlreadyRegisteredException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiResponseDto.failure(exception.getMessage())
        );
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ApiResponseDto> handlePasswordMismatch(
        PasswordMismatchException exception
    ) {
        return ResponseEntity.badRequest().body(
            ApiResponseDto.failure(exception.getMessage())
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
        HttpMessageNotReadableException exception
    ) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.of(
                "MALFORMED_REQUEST",
                "El cuerpo de la solicitud no es un JSON válido.",
                List.of()
            )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
        MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity.badRequest().body(
            ApiErrorResponse.of(
                "INVALID_PARAMETER",
                "El parámetro '" + exception.getName() + "' no tiene un formato válido.",
                List.of()
            )
        );
    }

    // Errores estándar de Spring MVC (404, 405, 415, parámetros faltantes...) conservan
    // su código HTTP; cualquier otro error responde 500 sin exponer detalles internos.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
        Exception exception
    ) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return ResponseEntity.status(status).body(
                ApiErrorResponse.of(
                    "HTTP_" + status.value(),
                    messageFor(status),
                    List.of()
                )
            );
        }

        ApiErrorResponse response = ApiErrorResponse.of(
            "INTERNAL_ERROR",
            "Ocurrió un error inesperado. Intenta de nuevo más tarde.",
            List.of()
        );
        log.error("Unexpected error: traceId={}", response.traceId(), exception);
        return ResponseEntity.internalServerError().body(response);
    }

    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "El método HTTP no está soportado para este recurso.";
            case 415 -> "El tipo de contenido de la solicitud no está soportado.";
            default -> "La solicitud no pudo procesarse.";
        };
    }
}
