package com.udea.reservas.plataformareservas.servicios.controller;

import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import com.udea.reservas.plataformareservas.common.ApiSuccessResponse;
import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;
import com.udea.reservas.plataformareservas.servicios.dto.validation.StrictIntegerDeserializer;
import com.udea.reservas.plataformareservas.servicios.service.ServicioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers/me/services")
@RequiredArgsConstructor
@Slf4j
public class ServicioController {

    private static final String REJECTED_LOG_MESSAGE =
        "Service request rejected: errorCode={}, traceId={}";

    private final ServicioService servicioService;

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<ServicioResponse>> crearServicio(
        @AuthenticationPrincipal UserPrincipal usuario,
        @Valid @RequestBody CrearServicioRequest request
    ) {
        ServicioResponse response = servicioService.crearServicio(usuario.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiSuccessResponse<>("Servicio creado exitosamente.", response));
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<ApiSuccessResponse<Void>> eliminarServicio(
        @AuthenticationPrincipal UserPrincipal usuario,
        @PathVariable Integer serviceId
    ) {
        servicioService.eliminarServicio(usuario.getId(), serviceId);
        return ResponseEntity.ok(
            new ApiSuccessResponse<>("Servicio eliminado exitosamente.", null)
        );
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> manejarReglaDeNegocio(
        BusinessRuleException exception
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
            exception.getErrorCode(),
            exception.getMessage(),
            List.of()
        );
        log.warn(REJECTED_LOG_MESSAGE, response.errorCode(), response.traceId());
        return ResponseEntity.status(exception.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> manejarValidacion(
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
        log.warn(REJECTED_LOG_MESSAGE, response.errorCode(), response.traceId());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> manejarSolicitudNoLegible(
        HttpMessageNotReadableException exception
    ) {
        boolean invalidInteger = exception.getMessage() != null
            && exception.getMessage().contains(StrictIntegerDeserializer.ERROR_MESSAGE);
        ApiErrorResponse response = invalidInteger
            ? ApiErrorResponse.of(
                "VALIDATION_ERROR",
                "Datos inválidos.",
                List.of(StrictIntegerDeserializer.ERROR_MESSAGE)
            )
            : ApiErrorResponse.of(
                "MALFORMED_REQUEST",
                "El cuerpo de la solicitud no es un JSON válido.",
                List.of()
            );
        log.warn(REJECTED_LOG_MESSAGE, response.errorCode(), response.traceId());
        return ResponseEntity.badRequest().body(response);
    }
}
