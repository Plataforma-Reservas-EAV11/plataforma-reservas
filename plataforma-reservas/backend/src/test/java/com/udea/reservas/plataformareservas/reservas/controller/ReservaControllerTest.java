package com.udea.reservas.plataformareservas.reservas.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.reservas.dto.CrearReservaRequest;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import com.udea.reservas.plataformareservas.usuarios.model.Usuario;
import java.time.LocalDate;
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
class ReservaControllerTest {

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private ReservaController reservaController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservaController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Crear una reserva válida retorna 201 y la reserva creada")
    void crearReservaValidaRetorna201() throws Exception {
        // Arrange
        CrearReservaRequest request = new CrearReservaRequest(
            1,
            10,
            LocalDate.of(2099, 1, 1),
            LocalTime.of(10, 30)
        );
        Reserva reserva = crearReserva(100, 1, 10, request, "activa");

        when(
            reservaService.crearReserva(
                request.usuarioId(),
                request.horarioId(),
                request.fecha(),
                request.hora()
            )
        ).thenReturn(reserva);

        // Act
        mockMvc
            .perform(
                post("/api/reservas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            // Assert
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.usuarioId").value(1))
            .andExpect(jsonPath("$.horarioId").value(10))
            .andExpect(jsonPath("$.fecha").value("2099-01-01"))
            .andExpect(jsonPath("$.hora").value("10:30:00"))
            .andExpect(jsonPath("$.estado").value("activa"));

        verify(reservaService).crearReserva(
            request.usuarioId(),
            request.horarioId(),
            request.fecha(),
            request.hora()
        );
    }

    @Test
    @DisplayName("Cancelar una reserva retorna 200 y confirma la cancelación")
    void cancelarReservaRetorna200() throws Exception {
        // Arrange
        Reserva reserva = crearReserva(
            100,
            1,
            10,
            new CrearReservaRequest(
                1,
                10,
                LocalDate.of(2099, 1, 1),
                LocalTime.of(10, 30)
            ),
            "cancelada"
        );
        when(reservaService.cancelarReserva(100, 1)).thenReturn(reserva);

        // Act
        mockMvc
            .perform(
                delete("/api/reservas/{reservaId}/cancelar", 100)
                    .queryParam("usuarioId", "1")
            )
            // Assert
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reservaId").value(100))
            .andExpect(jsonPath("$.estado").value("cancelada"))
            .andExpect(
                jsonPath("$.mensaje").value(
                    "Reserva cancelada correctamente. El horario queda disponible para otros usuarios."
                )
            );

        verify(reservaService).cancelarReserva(100, 1);
    }

    @Test
    @DisplayName("Crear una reserva inexistente retorna 404 con el mensaje del servicio")
    void crearReservaInexistenteRetorna404() throws Exception {
        // Arrange
        CrearReservaRequest request = new CrearReservaRequest(
            99,
            10,
            LocalDate.of(2099, 1, 1),
            LocalTime.of(10, 30)
        );
        when(
            reservaService.crearReserva(
                request.usuarioId(),
                request.horarioId(),
                request.fecha(),
                request.hora()
            )
        ).thenThrow(new ReservaException("El usuario no existe.", org.springframework.http.HttpStatus.NOT_FOUND));

        // Act
        mockMvc
            .perform(
                post("/api/reservas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            // Assert
            .andExpect(status().isNotFound())
            .andExpect(content().string("El usuario no existe."));
    }

    @Test
    @DisplayName("Cancelar una reserva inválida retorna 400 con el mensaje del servicio")
    void cancelarReservaInvalidaRetorna400() throws Exception {
        // Arrange
        when(reservaService.cancelarReserva(100, 1)).thenThrow(
            new ReservaException(
                "La reserva ya está cancelada.",
                org.springframework.http.HttpStatus.BAD_REQUEST
            )
        );

        // Act
        mockMvc
            .perform(
                delete("/api/reservas/{reservaId}/cancelar", 100)
                    .queryParam("usuarioId", "1")
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(content().string("La reserva ya está cancelada."));
    }

    @Test
    @DisplayName("Crear una reserva sin campos obligatorios retorna 400")
    void crearReservaSinCamposObligatoriosRetorna400() throws Exception {
        // Arrange
        String requestInvalido = "{}";

        // Act
        mockMvc
            .perform(
                post("/api/reservas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestInvalido)
            )
            // Assert
            .andExpect(status().isBadRequest());

        verify(reservaService, never()).crearReserva(
            null,
            null,
            null,
            null
        );
    }

    private Reserva crearReserva(
        int reservaId,
        int usuarioId,
        int horarioId,
        CrearReservaRequest request,
        String estado
    ) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        HorarioDisponible horario = new HorarioDisponible();
        horario.setId(horarioId);

        return Reserva.builder()
            .id(reservaId)
            .usuario(usuario)
            .horario(horario)
            .fecha(request.fecha())
            .hora(request.hora())
            .estado(estado)
            .build();
    }
}