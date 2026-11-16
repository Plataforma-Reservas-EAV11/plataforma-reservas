package com.udea.reservas.plataformareservas.recursos.controller;

import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.common.ApiSuccessResponse;
import com.udea.reservas.plataformareservas.recursos.dto.EstadoRecursoRequest;
import com.udea.reservas.plataformareservas.recursos.dto.GuardarRecursoRequest;
import com.udea.reservas.plataformareservas.recursos.dto.RecursoResponse;
import com.udea.reservas.plataformareservas.recursos.service.RecursoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers/me/resources")
@RequiredArgsConstructor
public class RecursoController {

    private final RecursoService recursoService;

    @PostMapping
    public ResponseEntity<ApiSuccessResponse<RecursoResponse>> crearRecurso(
        @AuthenticationPrincipal UserPrincipal usuario,
        @Valid @RequestBody GuardarRecursoRequest request
    ) {
        RecursoResponse response = recursoService.crearRecurso(usuario.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiSuccessResponse<>("Recurso creado exitosamente.", response));
    }

    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<RecursoResponse>>> listarRecursos(
        @AuthenticationPrincipal UserPrincipal usuario
    ) {
        return ResponseEntity.ok(
            new ApiSuccessResponse<>(
                "Recursos consultados exitosamente.",
                recursoService.listarRecursos(usuario.getId())
            )
        );
    }

    @GetMapping("/{recursoId}")
    public ResponseEntity<ApiSuccessResponse<RecursoResponse>> consultarRecurso(
        @AuthenticationPrincipal UserPrincipal usuario,
        @PathVariable Integer recursoId
    ) {
        return ResponseEntity.ok(
            new ApiSuccessResponse<>(
                "Recurso consultado exitosamente.",
                recursoService.consultarRecurso(usuario.getId(), recursoId)
            )
        );
    }

    @PutMapping("/{recursoId}")
    public ResponseEntity<ApiSuccessResponse<RecursoResponse>> actualizarRecurso(
        @AuthenticationPrincipal UserPrincipal usuario,
        @PathVariable Integer recursoId,
        @Valid @RequestBody GuardarRecursoRequest request
    ) {
        return ResponseEntity.ok(
            new ApiSuccessResponse<>(
                "Recurso actualizado exitosamente.",
                recursoService.actualizarRecurso(usuario.getId(), recursoId, request)
            )
        );
    }

    @PatchMapping("/{recursoId}/estado")
    public ResponseEntity<ApiSuccessResponse<RecursoResponse>> cambiarEstado(
        @AuthenticationPrincipal UserPrincipal usuario,
        @PathVariable Integer recursoId,
        @Valid @RequestBody EstadoRecursoRequest request
    ) {
        return ResponseEntity.ok(
            new ApiSuccessResponse<>(
                request.activo() ? "Recurso activado exitosamente." : "Recurso desactivado exitosamente.",
                recursoService.cambiarEstado(usuario.getId(), recursoId, request.activo())
            )
        );
    }

    @PutMapping("/{recursoId}/servicios/{servicioId}")
    public ResponseEntity<Void> asociarServicio(
        @AuthenticationPrincipal UserPrincipal usuario,
        @PathVariable Integer recursoId,
        @PathVariable Integer servicioId
    ) {
        recursoService.asociarServicio(usuario.getId(), recursoId, servicioId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{recursoId}/servicios/{servicioId}")
    public ResponseEntity<Void> retirarServicio(
        @AuthenticationPrincipal UserPrincipal usuario,
        @PathVariable Integer recursoId,
        @PathVariable Integer servicioId
    ) {
        recursoService.retirarServicio(usuario.getId(), recursoId, servicioId);
        return ResponseEntity.noContent().build();
    }

}
