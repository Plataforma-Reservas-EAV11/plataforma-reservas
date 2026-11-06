package com.udea.reservas.plataformareservas.servicios.controller;

import com.udea.reservas.plataformareservas.common.ApiErrorResponse;
import com.udea.reservas.plataformareservas.common.ApiSuccessResponse;
import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.servicios.dto.AvailableSlotResponse;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderServiceResponse;
import com.udea.reservas.plataformareservas.servicios.service.ServiceCatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ServiceCatalogController {

    private final ServiceCatalogService catalogService;

    @GetMapping("/providers/{providerId}/services")
    public ApiSuccessResponse<List<ProviderServiceResponse>> listProviderServices(
        @PathVariable Integer providerId
    ) {
        return new ApiSuccessResponse<>(
            "Servicios consultados correctamente.",
            catalogService.getProviderServices(providerId)
        );
    }

    @GetMapping("/services/{serviceId}/availability")
    public ApiSuccessResponse<List<AvailableSlotResponse>> listAvailableSlots(
        @PathVariable Integer serviceId
    ) {
        return new ApiSuccessResponse<>(
            "Horarios disponibles consultados correctamente.",
            catalogService.getAvailableSlots(serviceId)
        );
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
            "Catalog request rejected: errorCode={}, traceId={}",
            response.errorCode(),
            response.traceId()
        );
        return ResponseEntity.status(exception.getStatus()).body(response);
    }
}
