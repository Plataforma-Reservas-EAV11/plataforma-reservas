package com.udea.reservas.plataformareservas.proveedores.service;

import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationRequest;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationResponse;

public interface ProviderRegistrationService {
    ProviderRegistrationResponse register(ProviderRegistrationRequest request);
}
