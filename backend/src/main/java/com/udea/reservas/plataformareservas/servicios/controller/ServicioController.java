package com.udea.reservas.plataformareservas.servicios.controller;

import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.common.ApiSuccessResponse;
import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;
import com.udea.reservas.plataformareservas.servicios.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers/me/services")
@RequiredArgsConstructor
public class ServicioController {

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

    @DeleteMapping("/{servicioId}")
    public ResponseEntity<ApiSuccessResponse<Void>> eliminarServicio(
        @AuthenticationPrincipal UserPrincipal usuario,
        @PathVariable Integer servicioId
    ) {
        servicioService.eliminarServicio(usuario.getId(), servicioId);
        return ResponseEntity.ok(new ApiSuccessResponse<>("Servicio eliminado exitosamente.", null));
    }

}
