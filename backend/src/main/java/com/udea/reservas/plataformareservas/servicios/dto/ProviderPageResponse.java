package com.udea.reservas.plataformareservas.servicios.dto;

import java.util.List;

public record ProviderPageResponse(
    List<ProviderSummaryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last,
    boolean empty
) {}
