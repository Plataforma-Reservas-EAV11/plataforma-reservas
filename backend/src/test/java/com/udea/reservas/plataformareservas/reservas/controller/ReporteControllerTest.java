package com.udea.reservas.plataformareservas.reservas.controller;

import com.udea.reservas.plataformareservas.reservas.dto.ReporteOcupacionDTO;
import com.udea.reservas.plataformareservas.reservas.dto.RecursoUsoDTO;
import com.udea.reservas.plataformareservas.reservas.dto.ServicioOcupacionDTO;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void reporteSinReservasIncluyeMensajeYValoresVacios() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 30);
        when(reservaService.obtenerReporteOcupacion(inicio, fin))
                .thenReturn(new ReporteOcupacionDTO(0L, List.of(), List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(reporteController).build();

        mockMvc.perform(get("/api/reportes/ocupacion")
                        .param("fechaInicio", inicio.toString())
                        .param("fechaFin", fin.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.totalReservas").value(0))
                .andExpect(jsonPath("$.ocupacionPorServicio").isArray())
                .andExpect(jsonPath("$.ocupacionPorServicio").isEmpty())
                .andExpect(jsonPath("$.usoPorRecurso").isArray())
                .andExpect(jsonPath("$.usoPorRecurso").isEmpty())
                .andExpect(jsonPath("$.message").value("No hay reservas en el periodo seleccionado."));
    }

    @Test
    void reporteConReservasConservaDatosYOmiteMensaje() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 30);
        when(reservaService.obtenerReporteOcupacion(inicio, fin))
                .thenReturn(new ReporteOcupacionDTO(1L,
                        List.of(new ServicioOcupacionDTO("Corte", 1L)),
                        List.of(new RecursoUsoDTO("Silla 1", 1L))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(reporteController).build();

        mockMvc.perform(get("/api/reportes/ocupacion")
                        .param("fechaInicio", inicio.toString())
                        .param("fechaFin", fin.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.totalReservas").value(1))
                .andExpect(jsonPath("$.ocupacionPorServicio[0].nombreServicio").value("Corte"))
                .andExpect(jsonPath("$.ocupacionPorServicio[0].cantidadReservas").value(1))
                .andExpect(jsonPath("$.usoPorRecurso[0].nombreRecurso").value("Silla 1"))
                .andExpect(jsonPath("$.usoPorRecurso[0].cantidadUso").value(1))
                .andExpect(jsonPath("$.message").doesNotHaveJsonPath());
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
