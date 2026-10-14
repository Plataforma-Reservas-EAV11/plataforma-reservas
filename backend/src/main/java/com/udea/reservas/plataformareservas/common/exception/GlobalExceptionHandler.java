package com.udea.reservas.plataformareservas.common.exception;

import com.udea.reservas.plataformareservas.auth.exception.EmailAlreadyRegisteredException;
import com.udea.reservas.plataformareservas.auth.exception.PasswordMismatchException;
import com.udea.reservas.plataformareservas.common.dto.ApiResponseDto;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
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

}
