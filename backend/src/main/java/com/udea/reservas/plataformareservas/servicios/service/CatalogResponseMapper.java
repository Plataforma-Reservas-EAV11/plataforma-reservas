package com.udea.reservas.plataformareservas.servicios.service;

import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderServiceResponse;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderSummaryResponse;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import org.springframework.util.Assert;

final class CatalogResponseMapper {

    private static final String PROVIDER_MESSAGE =
        "A stored provider must have an identifier and business name.";
    private static final String SERVICE_MESSAGE =
        "A stored service must have an identifier, provider and positive duration.";

    private CatalogResponseMapper() {}

    static ProviderSummaryResponse toProviderSummary(Proveedor provider) {
        Proveedor stored = CatalogDataValidator.requirePresent(provider, PROVIDER_MESSAGE);
        Assert.state(stored.getId() != null, PROVIDER_MESSAGE);
        Assert.state(stored.getNombreNegocio() != null, PROVIDER_MESSAGE);
        return new ProviderSummaryResponse(stored.getId(), stored.getNombreNegocio());
    }

    static ProviderServiceResponse toProviderService(Servicio service, Integer agendaId) {
        Servicio stored = CatalogDataValidator.requirePresent(service, SERVICE_MESSAGE);
        Proveedor provider = CatalogDataValidator.requirePresent(
            stored.getProveedor(),
            SERVICE_MESSAGE
        );
        Assert.state(stored.getId() != null, SERVICE_MESSAGE);
        Assert.state(provider.getNombreNegocio() != null, SERVICE_MESSAGE);
        Assert.state(stored.getDuracionMinutos() != null, SERVICE_MESSAGE);
        Assert.state(stored.getDuracionMinutos() > 0, SERVICE_MESSAGE);
        return new ProviderServiceResponse(
            stored.getId(),
            stored.getNombre(),
            stored.getDuracionMinutos(),
            provider.getNombreNegocio(),
            agendaId
        );
    }
}
