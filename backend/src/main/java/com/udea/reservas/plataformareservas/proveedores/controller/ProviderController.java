package com.udea.reservas.plataformareservas.proveedores.controller;

import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import com.udea.reservas.plataformareservas.common.ApiSuccessResponse;
import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationRequest;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationResponse;
import com.udea.reservas.plataformareservas.proveedores.service.ProviderRegistrationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
@Slf4j
public class ProviderController {

    private static final String REJECTED_LOG_MESSAGE =
        "Provider request rejected: errorCode={}, traceId={}";

    private static final String SUCCESS_MESSAGE =
        "Registro de proveedor exitoso. Ya puedes iniciar sesión.";

    private final ProviderRegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<ProviderRegistrationResponse>> register(
        @Valid @RequestBody ProviderRegistrationRequest request
    ) {
        ProviderRegistrationResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiSuccessResponse<>(SUCCESS_MESSAGE, response));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRuleException(
        BusinessRuleException exception
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
            exception.getErrorCode(),
            exception.getMessage(),
            List.of()
        );
        log.warn(
            REJECTED_LOG_MESSAGE,
            response.errorCode(),
            response.traceId()
        );
        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
        MethodArgumentNotValidException exception
    ) {
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .toList();
        ApiErrorResponse response = ApiErrorResponse.of(
            "VALIDATION_ERROR",
            "Datos inválidos.",
            details
        );
        log.warn(
            REJECTED_LOG_MESSAGE,
            response.errorCode(),
            response.traceId()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
        HttpMessageNotReadableException exception
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
            "MALFORMED_REQUEST",
            "El cuerpo de la solicitud no es un JSON válido.",
            List.of()
        );
        log.warn(
            REJECTED_LOG_MESSAGE,
            response.errorCode(),
            response.traceId()
        );
        return ResponseEntity.badRequest().body(response);
    }
}
