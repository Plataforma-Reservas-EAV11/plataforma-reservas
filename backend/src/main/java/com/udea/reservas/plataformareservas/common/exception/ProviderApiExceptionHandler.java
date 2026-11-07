package com.udea.reservas.plataformareservas.common.exception;

import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.proveedores.controller.ProviderController;
import com.udea.reservas.plataformareservas.recursos.controller.RecursoController;
import com.udea.reservas.plataformareservas.servicios.controller.ServiceCatalogController;
import com.udea.reservas.plataformareservas.servicios.controller.ServicioController;
import com.udea.reservas.plataformareservas.servicios.dto.validation.StrictIntegerDeserializer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

@RestControllerAdvice(assignableTypes = {
    RecursoController.class, ServicioController.class, ProviderController.class,
    ServiceCatalogController.class
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProviderApiExceptionHandler {

    private final GlobalExceptionHandler globalExceptionHandler;

    public ProviderApiExceptionHandler(GlobalExceptionHandler globalExceptionHandler) {
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(
        BusinessRuleException exception, HandlerMethod handler
    ) {
        return respond(handler, exception.getStatus(), exception.getErrorCode(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException exception, HandlerMethod handler) {
        if (handler.getBeanType() == ServiceCatalogController.class) {
            return globalExceptionHandler.handleValidationException(exception);
        }
        List<String> details = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + " " + error.getDefaultMessage())
            .toList();
        return respond(handler, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos inválidos.", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformed(
        HttpMessageNotReadableException exception, HandlerMethod handler
    ) {
        if (handler.getBeanType() == ServiceCatalogController.class) {
            return globalExceptionHandler.handleMalformedRequest(exception);
        }
        if (handler.getBeanType() == ServicioController.class
            && exception.getMessage() != null
            && exception.getMessage().contains(StrictIntegerDeserializer.ERROR_MESSAGE)) {
            return respond(handler, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Datos inválidos.",
                List.of(StrictIntegerDeserializer.ERROR_MESSAGE));
        }
        return respond(handler, HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
            "El cuerpo de la solicitud no es un JSON válido.", List.of());
    }

    private ResponseEntity<ApiErrorResponse> respond(
        HandlerMethod handler, HttpStatus status, String code, String message, List<String> details
    ) {
        Class<?> controller = handler.getBeanType();
        String prefix;
        if (controller == RecursoController.class) {
            prefix = "Resource";
        } else if (controller == ServicioController.class) {
            prefix = "Service";
        } else if (controller == ProviderController.class) {
            prefix = "Provider";
        } else {
            prefix = "Catalog";
        }
        ApiErrorResponse response = ApiErrorResponse.of(code, message, details);
        Logger logger = LoggerFactory.getLogger(controller);
        logger.warn("{} request rejected: errorCode={}, traceId={}", prefix, response.errorCode(), response.traceId());
        return ResponseEntity.status(status).body(response);
    }
}
