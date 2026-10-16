package com.udea.reservas.plataformareservas.proveedores.dto;

public record ProviderRegistrationResponse(
    Integer providerId,
    Integer userId,
    String name,
    String email,
    String businessName,
    String phone
) {}
