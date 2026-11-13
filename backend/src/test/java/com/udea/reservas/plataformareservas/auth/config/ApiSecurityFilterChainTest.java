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
import com.udea.reservas.plataformareservas.proveedores.controller.ProviderController;
import com.udea.reservas.plataformareservas.proveedores.dto.ProviderRegistrationResponse;
import com.udea.reservas.plataformareservas.proveedores.service.ProviderRegistrationService;
import com.udea.reservas.plataformareservas.reservas.controller.ActiveReservationController;
import com.udea.reservas.plataformareservas.reservas.controller.ReservaController;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.service.ReservaService;
import com.udea.reservas.plataformareservas.recursos.controller.RecursoController;
import com.udea.reservas.plataformareservas.recursos.dto.GuardarRecursoRequest;
import com.udea.reservas.plataformareservas.recursos.dto.RecursoResponse;
import com.udea.reservas.plataformareservas.recursos.service.RecursoService;
import com.udea.reservas.plataformareservas.servicios.controller.ServicioController;
import com.udea.reservas.plataformareservas.servicios.controller.ServiceCatalogController;
import com.udea.reservas.plataformareservas.servicios.dto.CrearServicioRequest;
import com.udea.reservas.plataformareservas.servicios.dto.ServicioResponse;
import com.udea.reservas.plataformareservas.servicios.service.ServicioService;
import com.udea.reservas.plataformareservas.servicios.service.ServiceCatalogService;
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
    ProviderController.class,
    ActiveReservationController.class,
    ReservaController.class,
    HorarioDisponibleController.class,
    ServicioController.class,
    ServiceCatalogController.class,
    RecursoController.class
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
        "{\"servicioId\":10,\"fecha\":\"2099-01-01\",\"hora\":\"10:30:00\"}";
    private static final String HORARIO_BODY =
        "{\"diaSemana\":1,\"horaInicio\":\"08:00:00\",\"horaFin\":\"12:00:00\",\"duracionSlotMin\":30}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ProviderRegistrationService providerRegistrationService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private HorarioDisponibleService horarioService;

    @MockitoBean
    private ServicioService servicioService;

    @MockitoBean
    private ServiceCatalogService catalogService;

    @MockitoBean
    private RecursoService recursoService;

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
    @DisplayName("Consultar reservas activas sin sesión responde 401")
    void consultarReservasActivasSinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/me/active"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));

        verify(reservaService, never()).getActiveReservations(any());
    }

    @Test
    @DisplayName("Consultar reservas activas utiliza la identidad de sesión, no un userId enviado")
    void consultarReservasActivasUsaUsuarioDeSesion() throws Exception {
        MockHttpSession session = sesionAutenticada(7);
        when(reservaService.getActiveReservations(7)).thenReturn(java.util.List.of());

        mockMvc.perform(
            get("/api/v1/reservations/me/active")
                .session(session)
                .queryParam("userId", "99")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        verify(reservaService).getActiveReservations(7);
        verify(reservaService, never()).getActiveReservations(99);
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

    @Test
    @DisplayName("Crear un servicio sin sesión responde 401 y no llega al servicio")
    void crearServicioSinSesionResponde401() throws Exception {
        mockMvc.perform(
            post("/api/v1/providers/me/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SERVICIO_BODY)
        )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));

        verify(servicioService, never()).crearServicio(any(), any());
    }

    @Test
    @DisplayName("Las consultas de catálogo requieren sesión")
    void catalogQueriesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/providers"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/v1/providers/1/services"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/services/1/availability"))
            .andExpect(status().isUnauthorized());

        verify(catalogService, never()).getProviders(any(Integer.class), any(Integer.class));
        verify(catalogService, never()).getProviderServices(any(Integer.class));
        verify(catalogService, never()).getAvailableSlots(any(Integer.class));
    }

    @Test
    @DisplayName("Un usuario autenticado puede leer el catálogo sin rol de proveedor")
    void authenticatedUserCanReadCatalog() throws Exception {
        when(catalogService.getProviders(0, 20)).thenReturn(
            new com.udea.reservas.plataformareservas.servicios.dto.ProviderPageResponse(
                java.util.List.of(), 0, 20, 0, 0, true, true, true
            )
        );
        when(catalogService.getProviderServices(1)).thenReturn(java.util.List.of());
        when(catalogService.getAvailableSlots(1)).thenReturn(java.util.List.of());
        MockHttpSession session = sesionAutenticada(7, "usuario");

        mockMvc.perform(get("/api/v1/providers").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.empty").value(true));
        mockMvc.perform(get("/api/v1/providers/1/services").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
        mockMvc.perform(get("/api/v1/services/1/availability").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Consultar el perfil de proveedor sin sesión responde 401")
    void consultarPerfilProveedorSinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/providers/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));

        verify(providerRegistrationService, never()).getByUserId(any());
    }

    @Test
    @DisplayName("Un usuario cliente no puede consultar el perfil de proveedor")
    void clienteNoPuedeConsultarPerfilProveedor() throws Exception {
        MockHttpSession session = sesionAutenticada(7, "usuario");

        mockMvc.perform(get("/api/v1/providers/me").session(session))
            .andExpect(status().isForbidden());

        verify(providerRegistrationService, never()).getByUserId(any());
    }

    @Test
    @DisplayName("Un proveedor consulta su perfil con la identidad de la sesión")
    void proveedorConsultaPerfilConIdentidadDeSesion() throws Exception {
        MockHttpSession session = sesionAutenticada(7, "proveedor");
        when(providerRegistrationService.getByUserId(7)).thenReturn(
            new ProviderRegistrationResponse(
                47,
                7,
                "Ana",
                "ana@example.com",
                "MiNegocio",
                "3001234567"
            )
        );

        mockMvc.perform(
            get("/api/v1/providers/me")
                .session(session)
                .queryParam("userId", "99")
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Proveedor consultado correctamente."))
            .andExpect(jsonPath("$.data.providerId").value(47))
            .andExpect(jsonPath("$.data.businessName").value("MiNegocio"));

        verify(providerRegistrationService).getByUserId(7);
        verify(providerRegistrationService, never()).getByUserId(99);
    }

    @Test
    @DisplayName("Crear un servicio con sesión sin CSRF responde 403")
    void crearServicioSinCsrfResponde403() throws Exception {
        MockHttpSession session = sesionAutenticada(7, "proveedor");

        mockMvc.perform(
            post("/api/v1/providers/me/services")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(SERVICIO_BODY)
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));

        verify(servicioService, never()).crearServicio(any(), any());
    }

    @Test
    @DisplayName("Un usuario cliente autenticado no puede crear servicios")
    void crearServicioUsuarioNoProveedorResponde403() throws Exception {
        MockHttpSession session = sesionAutenticada(7, "usuario");
        JsonNode csrf = tokenCsrf(session);

        mockMvc.perform(
            post("/api/v1/providers/me/services")
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(SERVICIO_BODY)
        ).andExpect(status().isForbidden());

        verify(servicioService, never()).crearServicio(any(), any());
    }

    @Test
    @DisplayName("Un proveedor crea el servicio con la identidad de su sesión")
    void crearServicioProveedorAutenticadoResponde201() throws Exception {
        MockHttpSession session = sesionAutenticada(7, "proveedor");
        JsonNode csrf = tokenCsrf(session);
        CrearServicioRequest request = new CrearServicioRequest(
            "Corte",
            "Corte de cabello",
            30,
            List.of(51)
        );
        when(servicioService.crearServicio(7, request)).thenReturn(
            new ServicioResponse(41, 42, "Corte", "Corte de cabello", 30)
        );

        mockMvc.perform(
            post("/api/v1/providers/me/services")
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(SERVICIO_BODY)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(41))
            .andExpect(jsonPath("$.data.agendaId").value(42))
            .andExpect(jsonPath("$.data.proveedorId").doesNotExist());

        verify(servicioService).crearServicio(7, request);
    }

    @Test
    @DisplayName("Consultar recursos sin sesión responde 401")
    void consultarRecursosSinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/providers/me/resources"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));

        verify(recursoService, never()).listarRecursos(any());
    }

    @Test
    @DisplayName("Un usuario cliente no puede administrar recursos")
    void clienteNoPuedeAdministrarRecursos() throws Exception {
        MockHttpSession session = sesionAutenticada(7, "usuario");
        JsonNode csrf = tokenCsrf(session);

        mockMvc.perform(
            post("/api/v1/providers/me/resources")
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(RESOURCE_BODY)
        ).andExpect(status().isForbidden());

        verify(recursoService, never()).crearRecurso(any(), any());
    }

    @Test
    @DisplayName("Un proveedor necesita CSRF para crear recursos")
    void crearRecursoSinCsrfResponde403() throws Exception {
        MockHttpSession session = sesionAutenticada(7, "proveedor");

        mockMvc.perform(
            post("/api/v1/providers/me/resources")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(RESOURCE_BODY)
        )
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("CSRF_INVALID"));

        verify(recursoService, never()).crearRecurso(any(), any());
    }

    @Test
    @DisplayName("Un proveedor crea recursos con el usuario de la sesión")
    void crearRecursoConSesionYCsrfResponde201() throws Exception {
        MockHttpSession session = sesionAutenticada(7, "proveedor");
        JsonNode csrf = tokenCsrf(session);
        GuardarRecursoRequest request = new GuardarRecursoRequest("Consultorio", "sala");
        when(recursoService.crearRecurso(7, request))
            .thenReturn(new RecursoResponse(41, "Consultorio", "sala", true));

        mockMvc.perform(
            post("/api/v1/providers/me/resources")
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(RESOURCE_BODY)
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(41));

        verify(recursoService).crearRecurso(7, request);
    }

    @Test
    @DisplayName("El proveedor propietario sale de la sesión, no del payload")
    void crearServicioIgnoraProveedorIdEnviadoPorElCliente() throws Exception {
        MockHttpSession session = sesionAutenticada(7, "proveedor");
        JsonNode csrf = tokenCsrf(session);
        CrearServicioRequest request = new CrearServicioRequest(
            "Corte",
            "Corte de cabello",
            30,
            List.of(51)
        );
        when(servicioService.crearServicio(7, request)).thenReturn(
            new ServicioResponse(41, 42, "Corte", "Corte de cabello", 30)
        );

        mockMvc.perform(
            post("/api/v1/providers/me/services")
                .session(session)
                .header(csrf.get("headerName").asString(), csrf.get("token").asString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Corte\",\"descripcion\":\"Corte de cabello\"," +
                    "\"duracion\":30,\"recursoIds\":[51],\"proveedor_id\":999}"
                )
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.proveedorId").doesNotExist());

        verify(servicioService).crearServicio(7, request);
    }

    private static final String SERVICIO_BODY =
        "{\"nombre\":\"Corte\",\"descripcion\":\"Corte de cabello\"," +
        "\"duracion\":30,\"recursoIds\":[51]}";
    private static final String RESOURCE_BODY =
        "{\"nombre\":\"Consultorio\",\"tipo\":\"sala\"}";

    private MockHttpSession sesionAutenticada(int usuarioId) throws Exception {
        return sesionAutenticada(usuarioId, "usuario");
    }

    private MockHttpSession sesionAutenticada(int usuarioId, String role) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession();
        UserPrincipal principal = new UserPrincipal(
            usuarioId, "Ana", "ana@example.com", role, "hash", true
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
            )
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
