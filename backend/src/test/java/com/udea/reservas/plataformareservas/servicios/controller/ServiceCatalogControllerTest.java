package com.udea.reservas.plataformareservas.servicios.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.common.BusinessRuleException;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import com.udea.reservas.plataformareservas.common.exception.ProviderApiExceptionHandler;
import com.udea.reservas.plataformareservas.servicios.dto.AvailableSlotResponse;
import com.udea.reservas.plataformareservas.servicios.dto.ProviderServiceResponse;
import com.udea.reservas.plataformareservas.servicios.service.ServiceCatalogService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogControllerTest {

    @Mock
    private ServiceCatalogService catalogService;

    @InjectMocks
    private ServiceCatalogController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ProviderApiExceptionHandler(new GlobalExceptionHandler()), new GlobalExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("Devuelve servicios del proveedor en el contrato JSON del API")
    void listProviderServicesReturnsJsonResponse() throws Exception {
        when(catalogService.getProviderServices(8)).thenReturn(
            List.of(new ProviderServiceResponse(12, "Corte", 30, "Salón Central", 77))
        );

        mockMvc.perform(get("/api/v1/providers/8/services"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Servicios consultados correctamente."))
            .andExpect(jsonPath("$.data[0].id").value(12))
            .andExpect(jsonPath("$.data[0].nombre").value("Corte"))
            .andExpect(jsonPath("$.data[0].duracionMinutos").value(30))
            .andExpect(jsonPath("$.data[0].proveedorNombre").value("Salón Central"))
            .andExpect(jsonPath("$.data[0].agendaId").value(77));

        verify(catalogService).getProviderServices(8);
    }

    @Test
    @DisplayName("Devuelve data vacía para un proveedor sin servicios")
    void listProviderServicesReturnsEmptyArray() throws Exception {
        when(catalogService.getProviderServices(8)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/providers/8/services"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Devuelve 404 controlado cuando no existe el proveedor")
    void listProviderServicesReturnsNotFound() throws Exception {
        when(catalogService.getProviderServices(88)).thenThrow(
            new BusinessRuleException(
                "PROVIDER_NOT_FOUND",
                "El proveedor solicitado no existe.",
                HttpStatus.NOT_FOUND.value()
            )
        );

        mockMvc.perform(get("/api/v1/providers/88/services"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("PROVIDER_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Devuelve horarios disponibles con fecha y hora serializadas")
    void listAvailableSlotsReturnsJsonResponse() throws Exception {
        when(catalogService.getAvailableSlots(12)).thenReturn(
            List.of(new AvailableSlotResponse(
                51,
                LocalDate.of(2026, 11, 6),
                LocalTime.of(8, 0)
            ))
        );

        mockMvc.perform(get("/api/v1/services/12/availability"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message")
                .value("Horarios disponibles consultados correctamente."))
            .andExpect(jsonPath("$.data[0].horarioId").value(51))
            .andExpect(jsonPath("$.data[0].fecha").value("2026-11-06"))
            .andExpect(jsonPath("$.data[0].hora").value("08:00:00"));

        verify(catalogService).getAvailableSlots(12);
    }

    @Test
    @DisplayName("Devuelve data vacía cuando no quedan horarios libres")
    void listAvailableSlotsReturnsEmptyArray() throws Exception {
        when(catalogService.getAvailableSlots(12)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/services/12/availability"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Devuelve 404 controlado cuando no existe el servicio")
    void listAvailableSlotsReturnsNotFound() throws Exception {
        when(catalogService.getAvailableSlots(88)).thenThrow(
            new BusinessRuleException(
                "SERVICE_NOT_FOUND",
                "El servicio solicitado no existe.",
                HttpStatus.NOT_FOUND.value()
            )
        );

        mockMvc.perform(get("/api/v1/services/88/availability"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("SERVICE_NOT_FOUND"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("Un identificador de ruta no numérico responde 400")
    void nonNumericPathIdentifierReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/services/invalid/availability"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));
    }
}
