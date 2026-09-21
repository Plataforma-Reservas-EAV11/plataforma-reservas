package com.udea.reservas.plataformareservas.agendas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.service.HorarioDisponibleService;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class HorarioDisponibleControllerTest {

    @Mock
    private HorarioDisponibleService horarioDisponibleService;

    @InjectMocks
    private HorarioDisponibleController horarioDisponibleController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(
            horarioDisponibleController
        ).build();
        this.objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName(
        "Agregar horario a una agenda existente retorna respuesta exitosa"
    )
    void agregarHorarioExitoso() throws Exception {
        int agendaId = 7;

        CrearHorarioRequest request = new CrearHorarioRequest(
            2,
            LocalTime.of(10, 30),
            LocalTime.of(12, 0),
            90
        );

        HorarioResponse response = new HorarioResponse(
            1,
            agendaId,
            2,
            LocalTime.of(10, 30),
            LocalTime.of(12, 0),
            90
        );

        when(
            horarioDisponibleService.crearHorario(
                eq(agendaId),
                any(CrearHorarioRequest.class)
            )
        ).thenReturn(response);

        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", agendaId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(response.id()))
            .andExpect(jsonPath("$.agendaId").value(response.agendaId()))
            .andExpect(jsonPath("$.diaSemana").value(response.diaSemana()))
            .andExpect(jsonPath("$.horaInicio").value("10:30:00"))
            .andExpect(jsonPath("$.horaFin").value("12:00:00"))
            .andExpect(
                jsonPath("$.duracionSlotMin").value(response.duracionSlotMin())
            );

        verify(horarioDisponibleService).crearHorario(agendaId, request);
    }

    @Test
    @DisplayName(
        "Agregar horario sin body retorna 400 Bad Request"
    )
    void agregarHorarioSinBody() throws Exception {
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", 1)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName(
        "Agregar horario con body invalido retorna 400 Bad Request"
    )
    void agregarHorarioBodyInvalido() throws Exception {
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ \" campo invalido }")
            )
            .andExpect(status().isBadRequest());
    }
}
