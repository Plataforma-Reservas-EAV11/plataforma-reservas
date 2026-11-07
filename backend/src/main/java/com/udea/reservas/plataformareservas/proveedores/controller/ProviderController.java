package com.udea.reservas.plataformareservas.proveedores.controller;

import com.udea.reservas.plataformareservas.common.ApiSuccessResponse;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationRequest;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationResponse;
import com.udea.reservas.plataformareservas.proveedores.service.ProviderRegistrationService;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderPageResponse;
import com.udea.reservas.plataformareservas.servicios.service.ServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private static final String SUCCESS_MESSAGE =
        "Registro de proveedor exitoso. Ya puedes iniciar sesión.";

    private final ProviderRegistrationService registrationService;

    private final ServiceCatalogService serviceCatalogService;

    @GetMapping
    public ApiSuccessResponse<ProviderPageResponse> listProviders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return new ApiSuccessResponse<>(
            "Proveedores consultados correctamente.",
            serviceCatalogService.getProviders(page, size)
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiSuccessResponse<ProviderRegistrationResponse>> register(
        @Valid @RequestBody ProviderRegistrationRequest request
    ) {
        ProviderRegistrationResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiSuccessResponse<>(SUCCESS_MESSAGE, response));
    }

}
