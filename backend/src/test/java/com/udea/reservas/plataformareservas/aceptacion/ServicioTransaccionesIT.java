package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.repository.AgendaRepository;
import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"DB_PASSWORD=no-se-usa-con-testcontainers", "SESSION_COOKIE_SECURE=false"}
)
@Import(TestcontainersConfiguration.class)
class ServicioTransaccionesIT {

    private static final String RUTA = "/api/v1/providers/me/services";
    private static final String CLAVE = "clave-segura-1";

    @LocalServerPort
    private int puerto;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoSpyBean
    private IServicioRepository servicios;

    @MockitoSpyBean
    private IAgendaRepository agendas;

    private Integer usuarioId;
    private Integer proveedorId;
    private Integer recursoId;
    private String email;

    @BeforeEach
    void crearDatos() {
        email = "hu10-" + UUID.randomUUID() + "@test.com";
        usuarioId = jdbc.queryForObject(
            "INSERT INTO usuario (nombre, email, password_hash, rol) VALUES (?, ?, ?, ?) RETURNING id",
            Integer.class, "Proveedor HU-10", email, passwordEncoder.encode(CLAVE), "proveedor"
        );
        proveedorId = jdbc.queryForObject(
            "INSERT INTO proveedor (usuario_id, nombre_negocio) VALUES (?, ?) RETURNING id",
            Integer.class, usuarioId, "Negocio HU-10"
        );
        recursoId = jdbc.queryForObject(
            "INSERT INTO recurso (proveedor_id, nombre, tipo, activo) VALUES (?, ?, ?, true) RETURNING id",
            Integer.class, proveedorId, "Sala HU-10", "sala"
        );
    }

    @AfterEach
    void limpiarDatos() {
        reset(servicios);
        reset(agendas);
        if (proveedorId != null) {
            if (recursoId != null) {
                jdbc.update("DELETE FROM servicio_recurso WHERE recurso_id = ?", recursoId);
            }
            jdbc.update("DELETE FROM agenda WHERE proveedor_id = ?", proveedorId);
            jdbc.update("DELETE FROM servicio WHERE proveedor_id = ?", proveedorId);
            jdbc.update("DELETE FROM recurso WHERE proveedor_id = ?", proveedorId);
            jdbc.update("DELETE FROM proveedor WHERE id = ?", proveedorId);
        }
        if (usuarioId != null) {
            jdbc.update("DELETE FROM usuario WHERE id = ?", usuarioId);
        }
    }

    @Test
    void indiceResuelveCarreraTrasDosConsultasSinDuplicados() throws Exception {
        ClienteApi primero = clienteAutenticado("primero");
        ClienteApi segundo = clienteAutenticado("segundo");
        CyclicBarrier comprobaciones = new CyclicBarrier(2);
        ConcurrentLinkedQueue<Boolean> resultadosPrevios = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> nombresConsultados = new ConcurrentLinkedQueue<>();
        IServicioRepository serviciosReales =
            new JpaRepositoryFactory(entityManager).getRepository(IServicioRepository.class);

        doAnswer(invocation -> {
            boolean existe = serviciosReales.existsNombreActivo(
                invocation.getArgument(0), invocation.getArgument(1)
            );
            resultadosPrevios.add(existe);
            nombresConsultados.add(invocation.getArgument(1));
            comprobaciones.await(10, TimeUnit.SECONDS);
            return existe;
        }).when(servicios).existsNombreActivo(eq(proveedorId), anyString());

        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<HttpResponse<String>> respuestas;
        try {
            Future<HttpResponse<String>> primera = hilos.submit(() -> primero.postJson(RUTA, cuerpo("Corte")));
            Future<HttpResponse<String>> segunda = hilos.submit(() -> segundo.postJson(RUTA, cuerpo(" corte ")));
            respuestas = List.of(primera.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS));
        } finally {
            hilos.shutdownNow();
            assertThat(hilos.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(nombresConsultados).containsExactlyInAnyOrder("Corte", " corte ");
        assertThat(resultadosPrevios).containsExactlyInAnyOrder(false, false);
        assertThat(respuestas).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(201, 409);
        HttpResponse<String> conflicto = respuestas.stream()
            .filter(respuesta -> respuesta.statusCode() == 409).findFirst().orElseThrow();
        assertThat(primero.json(conflicto).get("errorCode").asString()).isEqualTo("DUPLICATE_SERVICE_NAME");
        assertThat(primero.json(conflicto).get("message").asString())
            .isEqualTo("Ya existe un servicio con ese nombre para este proveedor.");
        assertThat(contar("SELECT count(*) FROM servicio WHERE proveedor_id = ? AND NOT eliminado "
            + "AND lower(btrim(nombre)) = lower(btrim('Corte'))")).isEqualTo(1);
        assertThat(contarServicios()).isEqualTo(1);
        assertThat(contarAgendas()).isEqualTo(1);
        assertThat(contarAsociaciones()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM agenda a JOIN servicio s ON s.id = a.servicio_id "
                + "JOIN servicio_recurso sr ON sr.servicio_id = s.id "
                + "WHERE s.proveedor_id = ? AND a.proveedor_id = s.proveedor_id AND sr.recurso_id = ?",
            Integer.class, proveedorId, recursoId
        )).isEqualTo(1);
        assertThat(contar("SELECT count(*) FROM recurso WHERE proveedor_id = ?")).isEqualTo(1);
    }

    @Test
    void falloDespuesDePersistirRevierteServicioAgendaYAsociacion() {
        ClienteApi cliente = clienteAutenticado("proveedor");
        AtomicReference<List<Integer>> registrosAntesDelFallo = new AtomicReference<>();
        AgendaRepository agendasReales =
            new JpaRepositoryFactory(entityManager).getRepository(AgendaRepository.class);
        doAnswer(invocation -> {
            agendasReales.save(invocation.getArgument(0));
            agendasReales.flush();
            registrosAntesDelFallo.set(List.of(contarServicios(), contarAgendas(), contarAsociaciones()));
            throw new IllegalStateException("Fallo controlado de prueba despues de guardar la agenda");
        }).when(agendas).save(any(Agenda.class));

        HttpResponse<String> respuesta = cliente.postJson(RUTA, cuerpo("Servicio rollback"));

        assertThat(registrosAntesDelFallo.get()).containsExactly(1, 1, 1);
        assertThat(respuesta.statusCode()).isEqualTo(500);
        assertThat(cliente.json(respuesta).get("errorCode").asString()).isEqualTo("INTERNAL_ERROR");
        assertThat(contarServicios()).isZero();
        assertThat(contarAgendas()).isZero();
        assertThat(contarAsociaciones()).isZero();
        assertThat(contar("SELECT count(*) FROM recurso WHERE proveedor_id = ?")).isEqualTo(1);
    }

    @Test
    void duracionNoPositivaSeRechazaSinPersistir() {
        ClienteApi cliente = clienteAutenticado("proveedor");
        for (int duracion : new int[] {0, -1}) {
            Map<String, Object> solicitud = new HashMap<>(cuerpo("Servicio invalido"));
            solicitud.put("duracion", duracion);
            HttpResponse<String> respuesta = cliente.postJson(RUTA, solicitud);

            assertThat(respuesta.statusCode()).isEqualTo(400);
            var json = cliente.json(respuesta);
            assertThat(json.get("errorCode").asString()).isEqualTo("VALIDATION_ERROR");
            assertThat(json.get("details").size()).isEqualTo(1);
            assertThat(json.get("details").get(0).asString())
                .isEqualTo("duracion La duración en minutos debe ser un entero positivo.");
            assertThat(contarServicios()).isZero();
            assertThat(contarAgendas()).isZero();
            assertThat(contarAsociaciones()).isZero();
        }
    }

    private ClienteApi clienteAutenticado(String nombreSesion) {
        ClienteApi cliente = new ClienteApi();
        ReflectionTestUtils.setField(cliente, "puerto", puerto);
        assertThat(cliente.iniciarSesion(nombreSesion, email, CLAVE).statusCode()).isEqualTo(200);
        return cliente;
    }

    private Map<String, Object> cuerpo(String nombre) {
        return Map.of("nombre", nombre, "descripcion", "Descripcion de prueba",
            "duracion", 30, "recursoIds", List.of(recursoId));
    }

    private int contarServicios() {
        return contar("SELECT count(*) FROM servicio WHERE proveedor_id = ?");
    }

    private int contarAgendas() {
        return contar("SELECT count(*) FROM agenda WHERE proveedor_id = ?");
    }

    private int contarAsociaciones() {
        return jdbc.queryForObject(
            "SELECT count(*) FROM servicio_recurso WHERE recurso_id = ?", Integer.class, recursoId
        );
    }

    private int contar(String consulta) {
        return jdbc.queryForObject(consulta, Integer.class, proveedorId);
    }
}
