package com.udea.reservas.plataformareservas.reservas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.TestAuthentication;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.common.exception.GlobalExceptionHandler;
import com.udea.reservas.plataformareservas.reservas.dto.CrearReservaRequest;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import java.time.LocalDate;
import java.time.LocalTime;
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
class ReservaControllerTest {

    // Identidad de la sesión: el controlador la toma de aquí, no de la solicitud
    private static final int USUARIO_SESION = 1;

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private ReservaController reservaController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TestAuthentication.authenticateAs(
            TestAuthentication.principal(USUARIO_SESION)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(reservaController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(TestAuthentication.argumentResolver())
            .build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Crear una reserva válida retorna 201 y usa el usuario de la sesión")
    void crearReservaValidaRetorna201() throws Exception {
        // Arrange
        CrearReservaRequest request = new CrearReservaRequest(
            10,
            LocalDate.of(2099, 1, 1),
            LocalTime.of(10, 30)
        );
        Reserva reserva = crearReserva(100, USUARIO_SESION, 10, request, "activa");

        when(
            reservaService.crearReserva(
                USUARIO_SESION,
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
            .andExpect(jsonPath("$.usuarioId").value(USUARIO_SESION))
            .andExpect(jsonPath("$.horarioId").value(10))
            .andExpect(jsonPath("$.fecha").value("2099-01-01"))
            .andExpect(jsonPath("$.hora").value("10:30:00"))
            .andExpect(jsonPath("$.estado").value("activa"));

        verify(reservaService).crearReserva(
            USUARIO_SESION,
            request.horarioId(),
            request.fecha(),
            request.hora()
        );
    }

    @Test
    @DisplayName("Cancelar una reserva retorna 200 y usa el usuario de la sesión")
    void cancelarReservaRetorna200() throws Exception {
        // Arrange
        Reserva reserva = crearReserva(
            100,
            USUARIO_SESION,
            10,
            new CrearReservaRequest(
                10,
                LocalDate.of(2099, 1, 1),
                LocalTime.of(10, 30)
            ),
            "cancelada"
        );
        when(reservaService.cancelarReserva(100, USUARIO_SESION)).thenReturn(reserva);

        // Act
        mockMvc
            .perform(delete("/api/reservas/{reservaId}/cancelar", 100))
            // Assert
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reservaId").value(100))
            .andExpect(jsonPath("$.estado").value("cancelada"))
            .andExpect(
                jsonPath("$.mensaje").value(
                    "Reserva cancelada correctamente. El horario queda disponible para otros usuarios."
                )
            );

        verify(reservaService).cancelarReserva(100, USUARIO_SESION);
    }

    @Test
    @DisplayName("DEF-01: un usuarioId enviado en la URL se ignora; manda la sesión")
    void cancelarReservaIgnoraUsuarioIdDeLaSolicitud() throws Exception {
        // Arrange
        int usuarioAjeno = 2;
        when(reservaService.cancelarReserva(100, USUARIO_SESION)).thenThrow(
            new ReservaException(
                "No puedes cancelar una reserva que no te pertenece.",
                HttpStatus.FORBIDDEN
            )
        );

        // Act
        mockMvc
            .perform(
                delete("/api/reservas/{reservaId}/cancelar", 100)
                    .queryParam("usuarioId", String.valueOf(usuarioAjeno))
            )
            // Assert
            .andExpect(status().isForbidden());

        verify(reservaService).cancelarReserva(100, USUARIO_SESION);
        verify(reservaService, never()).cancelarReserva(100, usuarioAjeno);
    }

    @Test
    @DisplayName("Crear una reserva con un usuario que ya no existe retorna 404 con el mensaje del servicio")
    void crearReservaConUsuarioInexistenteRetorna404() throws Exception {
        // Arrange
        CrearReservaRequest request = new CrearReservaRequest(
            10,
            LocalDate.of(2099, 1, 1),
            LocalTime.of(10, 30)
        );
        when(
            reservaService.crearReserva(
                USUARIO_SESION,
                request.horarioId(),
                request.fecha(),
                request.hora()
            )
        ).thenThrow(new ReservaException("El usuario no existe.", HttpStatus.NOT_FOUND));

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
        when(reservaService.cancelarReserva(100, USUARIO_SESION)).thenThrow(
            new ReservaException(
                "La reserva ya está cancelada.",
                HttpStatus.BAD_REQUEST
            )
        );

        // Act
        mockMvc
            .perform(delete("/api/reservas/{reservaId}/cancelar", 100))
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(content().string("La reserva ya está cancelada."));
    }

    @Test
    @DisplayName("Cancelar una reserva con un id no numérico retorna 400")
    void cancelarReservaConIdInvalidoRetorna400() throws Exception {
        // Arrange
        String reservaIdInvalido = "no-numerico";

        // Act
        mockMvc
            .perform(delete("/api/reservas/{reservaId}/cancelar", reservaIdInvalido))
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
            .andExpect(
                jsonPath("$.message").value(
                    "El parámetro 'reservaId' no tiene un formato válido."
                )
            );

        verify(reservaService, never()).cancelarReserva(anyInt(), any());
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
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errors.horarioId").exists())
            .andExpect(jsonPath("$.errors.fecha").exists())
            .andExpect(jsonPath("$.errors.hora").exists());

        verify(reservaService, never()).crearReserva(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Crear una reserva con JSON inválido retorna 400")
    void crearReservaConJsonInvalidoRetorna400() throws Exception {
        // Arrange
        String requestInvalido = "{ \"horarioId\": 1,";

        // Act
        mockMvc
            .perform(
                post("/api/reservas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestInvalido)
            )
            // Assert
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));

        verify(reservaService, never()).crearReserva(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Crear una reserva sobre un horario ocupado retorna 409")
    void crearReservaHorarioOcupadoRetorna409() throws Exception {
        // Arrange
        CrearReservaRequest request = new CrearReservaRequest(
            10,
            LocalDate.of(2099, 1, 1),
            LocalTime.of(10, 30)
        );
        when(
            reservaService.crearReserva(
                USUARIO_SESION,
                request.horarioId(),
                request.fecha(),
                request.hora()
            )
        ).thenThrow(
            new ReservaException(
                "El recurso no está disponible en la fecha y hora seleccionadas.",
                HttpStatus.CONFLICT
            )
        );

        // Act
        mockMvc
            .perform(
                post("/api/reservas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            // Assert
            .andExpect(status().isConflict())
            .andExpect(
                content().string(
                    "El recurso no está disponible en la fecha y hora seleccionadas."
                )
            );
    }

    @Test
    @DisplayName("Cancelar una reserva ajena retorna 403")
    void cancelarReservaAjenaRetorna403() throws Exception {
        // Arrange
        when(reservaService.cancelarReserva(100, USUARIO_SESION)).thenThrow(
            new ReservaException(
                "No puedes cancelar una reserva que no te pertenece.",
                HttpStatus.FORBIDDEN
            )
        );

        // Act
        mockMvc
            .perform(delete("/api/reservas/{reservaId}/cancelar", 100))
            // Assert
            .andExpect(status().isForbidden())
            .andExpect(
                content().string("No puedes cancelar una reserva que no te pertenece.")
            );
    }

    @Test
    @DisplayName("Usar un método HTTP no soportado retorna 405 sin detalles internos")
    void metodoNoSoportadoRetorna405() throws Exception {
        // Arrange
        String ruta = "/api/reservas";

        // Act
        mockMvc
            .perform(get(ruta))
            // Assert
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.errorCode").value("HTTP_405"))
            .andExpect(
                jsonPath("$.message").value(
                    "El método HTTP no está soportado para este recurso."
                )
            );

        verify(reservaService, never()).crearReserva(any(), any(), any(), any());
    }

    private Reserva crearReserva(
        int reservaId,
        int usuarioId,
        int horarioId,
        CrearReservaRequest request,
        String estado
    ) {
        User usuario = new User();
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
