package com.udea.reservas.plataformareservas.auth.controller;

import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import com.udea.reservas.plataformareservas.common.dto.ApiResponseDto;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errores propios de {@link UserController}. Tiene precedencia sobre el
 * GlobalExceptionHandler para que su manejador genérico de Exception no los
 * capture, y se limita a este controlador porque la violación de integridad se
 * interpreta aquí como "correo ya registrado".
 */
@RestControllerAdvice(assignableTypes = UserController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
        BadCredentialsException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ApiErrorResponse.of(
                "INVALID_CREDENTIALS",
                "El correo electrónico o la contraseña no son correctos.",
                List.of()
            )
        );
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLockedAccount(
        LockedException exception
    ) {
        return ResponseEntity.status(HttpStatus.LOCKED).body(
            ApiErrorResponse.of(
                "ACCOUNT_LOCKED",
                "La cuenta está bloqueada temporalmente por demasiados intentos fallidos. Intenta de nuevo más tarde.",
                List.of()
            )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseDto> handleRegistrationConflict(
        DataIntegrityViolationException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            ApiResponseDto.failure(
                "El correo electrónico ya está registrado."
            )
        );
    }
}
