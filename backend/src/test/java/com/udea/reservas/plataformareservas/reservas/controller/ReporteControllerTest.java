package com.udea.reservas.plataformareservas.reservas.controller;

import com.udea.reservas.plataformareservas.reservas.dto.ReporteOcupacionDTO;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private ReporteController reporteController;

    @Test
    void obtenerReporteOcupacion_RespuestaExitosa() {
        // Arrange
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 30);
        ReporteOcupacionDTO mockReporte = new ReporteOcupacionDTO(5L, List.of(), List.of());

        when(reservaService.obtenerReporteOcupacion(inicio, fin)).thenReturn(mockReporte);

        // Act
        ResponseEntity<Object> response = reporteController.obtenerReporteOcupacion(inicio, fin);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockReporte, response.getBody());
    }

    @Test
    void obtenerReporteOcupacion_RangoInvalido_RetornaBadRequest() {
        // Arrange
        LocalDate inicio = LocalDate.of(2026, 12, 1);
        LocalDate fin = LocalDate.of(2026, 11, 1);

        when(reservaService.obtenerReporteOcupacion(inicio, fin))
                .thenThrow(new IllegalArgumentException("El rango de fechas no es válido."));

        // Act
        ResponseEntity<Object> response = reporteController.obtenerReporteOcupacion(inicio, fin);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("El rango de fechas no es válido.", response.getBody());
    }
}
