package com.udea.reservas.plataformareservas.servicios.controller;

import com.udea.reservas.plataformareservas.common.ApiSuccessResponse;
import com.udea.reservas.plataformareservas.servicios.dto.AvailableSlotResponse;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderServiceResponse;
import com.udea.reservas.plataformareservas.servicios.service.ServiceCatalogService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
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

}
