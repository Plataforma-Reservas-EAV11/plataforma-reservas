package com.udea.reservas.plataformareservas.reservas.controller;

import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.reservas.dto.ActiveReservationResponse;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ActiveReservationController {

    private final ReservaService reservaService;

    @GetMapping("/api/v1/reservations/me/active")
    public List<ActiveReservationResponse> getActiveReservations(
        @AuthenticationPrincipal UserPrincipal user
    ) {
        return reservaService.getActiveReservations(user.getId());
    }
}
