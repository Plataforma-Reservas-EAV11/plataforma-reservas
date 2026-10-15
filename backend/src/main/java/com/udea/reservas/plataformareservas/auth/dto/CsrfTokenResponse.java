package com.udea.reservas.plataformareservas.auth.dto;

public record CsrfTokenResponse(
    String headerName,
    String parameterName,
    String token
) {}
