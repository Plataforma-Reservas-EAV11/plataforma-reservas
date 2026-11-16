package com.udea.reservas.plataformareservas.agendas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.udea.reservas.plataformareservas.TestAuthentication;
import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.exception.AgendaException;
import com.udea.reservas.plataformareservas.agendas.service.HorarioDisponibleService;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class HorarioDisponibleControllerTest {

    // Identidad de la sesión: el controlador la toma de aquí, no de la solicitud
    private static final int USUARIO_SESION = 1;

    @Mock
    private HorarioDisponibleService horarioDisponibleService;

    @InjectMocks
    private HorarioDisponibleController horarioDisponibleController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        TestAuthentication.authenticateAs(
            TestAuthentication.principal(USUARIO_SESION)
        );
        this.mockMvc = MockMvcBuilders.standaloneSetup(
            horarioDisponibleController
        )
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(TestAuthentication.argumentResolver())
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName(
        "Agregar horario a una agenda existente retorna respuesta exitosa"
    )
    void agregarHorarioExitoso() throws Exception {
        // Arrange
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
                eq(USUARIO_SESION),
                any(CrearHorarioRequest.class)
            )
        ).thenReturn(response);

        // Act
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", agendaId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(request))
            )
            // Assert
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.mensaje").value(
                    "Bloque de disponibilidad creado exitosamente."
                )
            )
            .andExpect(jsonPath("$.data.id").value(response.id()))
            .andExpect(jsonPath("$.data.agendaId").value(response.agendaId()))
            .andExpect(jsonPath("$.data.diaSemana").value(response.diaSemana()))
            .andExpect(jsonPath("$.data.horaInicio").value("10:30:00"))
            .andExpect(jsonPath("$.data.horaFin").value("12:00:00"))
            .andExpect(
                jsonPath("$.data.duracionSlotMin").value(
                    response.duracionSlotMin()
                )
            );

        verify(horarioDisponibleService).crearHorario(agendaId, USUARIO_SESION, request);
    }

    @Test
    @DisplayName(
        "Agregar horario sin body retorna 400 Bad Request"
    )
    void agregarHorarioSinBody() throws Exception {
        // Arrange
        int agendaId = 1;

        // Act
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", agendaId)
                    .contentType(MediaType.APPLICATION_JSON)
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));

        verify(horarioDisponibleService, never()).crearHorario(anyInt(), any(), any());
    }

    @Test
    @DisplayName(
        "Agregar horario con body invalido retorna 400 Bad Request"
    )
    void agregarHorarioBodyInvalido() throws Exception {
        // Arrange
        String bodyInvalido = "{ \" campo invalido }";

        // Act
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", 1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(bodyInvalido)
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));

        verify(horarioDisponibleService, never()).crearHorario(anyInt(), any(), any());
    }

    @Test
    @DisplayName("Agregar horario sin hora de fin retorna 400 con el campo inválido")
    void agregarHorarioSinHoraFin() throws Exception {
        // Arrange
        String body = """
            { "diaSemana": 1, "horaInicio": "08:00:00", "duracionSlotMin": 60 }
            """;

        // Act
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", 3)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(
                jsonPath("$.errors.horaFin").value("La hora de fin es obligatoria")
            );

        verify(horarioDisponibleService, never()).crearHorario(anyInt(), any(), any());
    }

    @Test
    @DisplayName("Agregar horario con día de la semana 7 y duración 0 retorna 400")
    void agregarHorarioConValoresFueraDeRango() throws Exception {
        // Arrange
        CrearHorarioRequest request = new CrearHorarioRequest(
            7,
            LocalTime.of(8, 0),
            LocalTime.of(12, 0),
            0
        );

        // Act
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", 3)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.diaSemana").exists())
            .andExpect(jsonPath("$.errors.duracionSlotMin").exists());

        verify(horarioDisponibleService, never()).crearHorario(anyInt(), any(), any());
    }

    @Test
    @DisplayName("Agregar horario a una agenda inexistente retorna 404 con el mensaje del servicio")
    void agregarHorarioAgendaInexistente() throws Exception {
        // Arrange
        CrearHorarioRequest request = new CrearHorarioRequest(
            1,
            LocalTime.of(8, 0),
            LocalTime.of(12, 0),
            30
        );
        when(horarioDisponibleService.crearHorario(99, USUARIO_SESION, request)).thenThrow(
            new AgendaException("La agenda con ID 99 no existe.", HttpStatus.NOT_FOUND)
        );

        // Act
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", 99)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            // Assert
            .andExpect(status().isNotFound())
            .andExpect(content().string("La agenda con ID 99 no existe."));
    }

    @Test
    @DisplayName("Agregar horario solapado retorna 400 con el mensaje del servicio")
    void agregarHorarioSolapado() throws Exception {
        // Arrange
        CrearHorarioRequest request = new CrearHorarioRequest(
            1,
            LocalTime.of(11, 0),
            LocalTime.of(13, 0),
            30
        );
        when(horarioDisponibleService.crearHorario(3, USUARIO_SESION, request)).thenThrow(
            new AgendaException(
                "El horario ya tiene disponibilidad definida. No se pueden solapar bloques.",
                HttpStatus.BAD_REQUEST
            )
        );

        // Act
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", 3)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(
                content().string(
                    "El horario ya tiene disponibilidad definida. No se pueden solapar bloques."
                )
            );
    }

    @Test
    @DisplayName("Agregar horario con un id de agenda no numérico retorna 400")
    void agregarHorarioConAgendaNoNumerica() throws Exception {
        // Arrange
        CrearHorarioRequest request = new CrearHorarioRequest(
            1,
            LocalTime.of(8, 0),
            LocalTime.of(12, 0),
            30
        );

        // Act
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", "abc")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));

        verify(horarioDisponibleService, never()).crearHorario(anyInt(), any(), any());
    }

    @Test
    @DisplayName("Agregar horario a la agenda de otro proveedor retorna 403 con el mensaje del servicio")
    void agregarHorarioAgendaAjena() throws Exception {
        // Arrange
        CrearHorarioRequest request = new CrearHorarioRequest(
            1,
            LocalTime.of(8, 0),
            LocalTime.of(12, 0),
            30
        );
        when(horarioDisponibleService.crearHorario(4, USUARIO_SESION, request)).thenThrow(
            new AgendaException(
                "No puedes modificar una agenda que no te pertenece.",
                HttpStatus.FORBIDDEN
            )
        );

        // Act
        mockMvc
            .perform(
                post("/api/agendas/{agendaId}/horarios", 4)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            // Assert
            .andExpect(status().isForbidden())
            .andExpect(
                content().string("No puedes modificar una agenda que no te pertenece.")
            );
    }

    @Test
    @DisplayName("Consultar los horarios de una agenda propia retorna la lista de bloques")
    void listarHorariosExitoso() throws Exception {
        // Arrange
        int agendaId = 7;
        List<HorarioResponse> horarios = List.of(
            new HorarioResponse(
                10,
                agendaId,
                1,
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                30
            ),
            new HorarioResponse(
                11,
                agendaId,
                3,
                LocalTime.of(14, 0),
                LocalTime.of(18, 0),
                60
            )
        );
        when(horarioDisponibleService.listarHorarios(agendaId, USUARIO_SESION))
            .thenReturn(horarios);

        // Act
        mockMvc
            .perform(get("/api/agendas/{agendaId}/horarios", agendaId))
            // Assert
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.mensaje").value("Horarios consultados exitosamente")
            )
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(10))
            .andExpect(jsonPath("$.data[0].agendaId").value(agendaId))
            .andExpect(jsonPath("$.data[0].diaSemana").value(1))
            .andExpect(jsonPath("$.data[0].horaInicio").value("08:00:00"))
            .andExpect(jsonPath("$.data[0].horaFin").value("12:00:00"))
            .andExpect(jsonPath("$.data[0].duracionSlotMin").value(30))
            .andExpect(jsonPath("$.data[1].diaSemana").value(3))
            .andExpect(jsonPath("$.data[1].horaInicio").value("14:00:00"));

        verify(horarioDisponibleService).listarHorarios(agendaId, USUARIO_SESION);
    }

    @Test
    @DisplayName("Consultar los horarios de una agenda sin bloques retorna una lista vacía")
    void listarHorariosDeAgendaSinBloques() throws Exception {
        // Arrange
        when(horarioDisponibleService.listarHorarios(5, USUARIO_SESION))
            .thenReturn(List.of());

        // Act
        mockMvc
            .perform(get("/api/agendas/{agendaId}/horarios", 5))
            // Assert
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.mensaje").value("Horarios consultados exitosamente")
            )
            .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("Consultar los horarios de una agenda inexistente retorna 404 con el mensaje del servicio")
    void listarHorariosAgendaInexistente() throws Exception {
        // Arrange
        when(horarioDisponibleService.listarHorarios(99, USUARIO_SESION))
            .thenThrow(
                new AgendaException(
                    "La agenda con ID 99 no existe.",
                    HttpStatus.NOT_FOUND
                )
            );

        // Act
        mockMvc
            .perform(get("/api/agendas/{agendaId}/horarios", 99))
            // Assert
            .andExpect(status().isNotFound())
            .andExpect(content().string("La agenda con ID 99 no existe."));
    }

    @Test
    @DisplayName("Consultar los horarios de la agenda de otro proveedor retorna 403 con el mensaje del servicio")
    void listarHorariosAgendaAjena() throws Exception {
        // Arrange
        when(horarioDisponibleService.listarHorarios(4, USUARIO_SESION))
            .thenThrow(
                new AgendaException(
                    "No puedes consultar una agenda que no te pertenece.",
                    HttpStatus.FORBIDDEN
                )
            );

        // Act
        mockMvc
            .perform(get("/api/agendas/{agendaId}/horarios", 4))
            // Assert
            .andExpect(status().isForbidden())
            .andExpect(
                content().string(
                    "No puedes consultar una agenda que no te pertenece."
                )
            );
    }

    @Test
    @DisplayName("Consultar horarios con un id de agenda no numérico retorna 400")
    void listarHorariosConAgendaNoNumerica() throws Exception {
        // Act
        mockMvc
            .perform(get("/api/agendas/{agendaId}/horarios", "abc"))
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));

        verify(horarioDisponibleService, never()).listarHorarios(anyInt(), any());
    }
}
