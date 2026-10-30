package com.udea.reservas.plataformareservas.auth.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.udea.reservas.plataformareservas.agendas.controller.HorarioDisponibleController;
import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.service.HorarioDisponibleService;
import com.udea.reservas.plataformareservas.auth.controller.UserController;
import com.udea.reservas.plataformareservas.auth.service.UserPrincipal;
import com.udea.reservas.plataformareservas.auth.service.UserService;
import com.udea.reservas.plataformareservas.reservas.controller.ReservaController;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** DEF-01: reservas y agendas exigen sesión; las operaciones de escritura, además, token CSRF. */
@WebMvcTest({
    UserController.class,
    ReservaController.class,
    HorarioDisponibleController.class
})
@Import({
    SecurityConfiguration.class,
    ApiSecurityConfiguration.class,
    JsonSecurityErrorHandler.class,
    PasswordEncoderConfiguration.class
})
@EnableWebSecurity
class ApiSecurityFilterChainTest {

    private static final String RESERVA_BODY =
        "{\"horarioId\":10,\"fecha\":\"2099-01-01\",\"hora\":\"10:30:00\"}";
    private static final String HORARIO_BODY =
        "{\"diaSemana\":1,\"horaInicio\":\"08:00:00\",\"horaFin\":\"12:00:00\",\"duracionSlotMin\":30}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private HorarioDisponibleService horarioService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Crear una reserva sin sesión responde 401 y no llega al servicio")
    void crearReservaSinSesionResponde401() throws Exception {
        mockMvc.perform(
            post("/api/reservas").contentType(MediaType.APPLICATION_JSON).content(RESERVA_BODY)
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));

        verify(reservaService, never()).crearReserva(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cancelar una reserva sin sesión responde 401 y no llega al servicio")
    void cancelarReservaSinSesionResponde401() throws Exception {
        mockMvc.perform(delete("/api/reservas/{id}/cancelar", 1))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));

        verify(reservaService, never()).cancelarReserva(any(), any());
    }

    @Test
    @DisplayName("Agregar un horario sin sesión responde 401 y no llega al servicio")
    void agregarHorarioSinSesionResponde401() throws Exception {
        mockMvc.perform(
            post("/api/agendas/{id}/horarios", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(HORARIO_BODY)
        )
            .andExpect(status().isUnauthorized());

        verify(horarioService, never()).crearHorario(any(), any(), any());
    }

    @Test
    @DisplayName("Con sesión pero sin token CSRF responde 403 y no llega al servicio")
    void conSesionSinCsrfResponde403() throws Exception {
        MockHttpSession session = sesionAutenticada(7);

        mockMvc.perform(
            post("/api/reservas")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(RESERVA_BODY)
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));

        verify(reservaService, never()).crearReserva(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Con sesión y token CSRF crea la reserva con el usuario de la sesión")
    void conSesionYCsrfCreaLaReservaDelUsuarioDeLaSesion() throws Exception {
        MockHttpSession session = sesionAutenticada(7);
        JsonNode csrf = tokenCsrf(session);
        User usuario = new User();
        usuario.setId(7);
        HorarioDisponible horario = new HorarioDisponible();
        horario.setId(10);
        Reserva reserva = Reserva.builder()
            .id(100)
            .usuario(usuario)
            .horario(horario)
            .fecha(LocalDate.of(2099, 1, 1))
            .hora(LocalTime.of(10, 30))
            .estado("activa")
            .build();
        when(
            reservaService.crearReserva(7, 10, LocalDate.of(2099, 1, 1), LocalTime.of(10, 30))
        ).thenReturn(reserva);

        mockMvc.perform(
            post("/api/reservas")
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(RESERVA_BODY)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.usuarioId").value(7));
    }

    @Test
    @DisplayName("Con sesión y token CSRF cancela con el usuario de la sesión, ignorando el de la URL")
    void conSesionYCsrfCancelaConElUsuarioDeLaSesion() throws Exception {
        MockHttpSession session = sesionAutenticada(7);
        JsonNode csrf = tokenCsrf(session);
        User usuario = new User();
        usuario.setId(7);
        HorarioDisponible horario = new HorarioDisponible();
        horario.setId(10);
        when(reservaService.cancelarReserva(100, 7)).thenReturn(
            Reserva.builder().id(100).usuario(usuario).horario(horario).estado("cancelada").build()
        );

        mockMvc.perform(
            delete("/api/reservas/{id}/cancelar", 100)
                .queryParam("usuarioId", "99")
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("cancelada"));

        verify(reservaService).cancelarReserva(100, 7);
    }

    @Test
    @DisplayName("Con sesión y token CSRF agrega el horario con el usuario de la sesión")
    void conSesionYCsrfAgregaElHorarioDelProveedorDeLaSesion() throws Exception {
        MockHttpSession session = sesionAutenticada(5);
        JsonNode csrf = tokenCsrf(session);
        when(horarioService.crearHorario(1, 5, new CrearHorarioRequest(1, LocalTime.of(8, 0), LocalTime.of(12, 0), 30)))
            .thenReturn(new HorarioResponse(1, 1, 1, LocalTime.of(8, 0), LocalTime.of(12, 0), 30));

        mockMvc.perform(
            post("/api/agendas/{id}/horarios", 1)
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(HORARIO_BODY)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.agendaId").value(1));
    }

    private MockHttpSession sesionAutenticada(int usuarioId) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession();
        UserPrincipal principal = new UserPrincipal(
            usuarioId, "Ana", "ana@example.com", "usuario", "hash", true
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of())
        );
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            context
        );
        return session;
    }

    private JsonNode tokenCsrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
