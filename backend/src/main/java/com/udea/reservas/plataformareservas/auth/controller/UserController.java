package com.udea.reservas.plataformareservas.auth.controller;

import com.udea.reservas.plataformareservas.auth.dto.UserRegistrationRequest;
import com.udea.reservas.plataformareservas.auth.service.UserService;
import com.udea.reservas.plataformareservas.common.dto.ApiResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto> register(
        @Valid @RequestBody UserRegistrationRequest registrationRequest
    ) {
        userService.register(registrationRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponseDto.success(
                "Registro exitoso. Tu cuenta fue creada."
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
