package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@RequiredArgsConstructor
public class PasosServicios {

    private static final String RUTA_SERVICIOS = "/api/v1/providers/me/services";

    /**
     * Identificador de relleno para los escenarios que se rechazan antes de validar
     * los recursos (sin sesión, rol incorrecto, sin token CSRF o sin perfil de
     * proveedor), donde solo hace falta que el cuerpo pase la validación de campos.
     */
    private static final List<Integer> RECURSOS_DE_RELLENO = List.of(1);

    private final ClienteApi api;
    private final DatosPrueba datos;

    private Integer usuarioId;
    private Integer recursoId;
    private final Map<String, Integer> serviciosPorNombre = new HashMap<>();

    @Dado("que existe el proveedor {string} con correo {string}")
    public void existeProveedor(String nombre, String email) {
        usuarioId = datos.crearProveedor(nombre, email);
        api.iniciarSesion(nombre, email, DatosPrueba.CONTRASENA_POR_DEFECTO);
    }

    @Dado("que el proveedor tiene el recurso {string}")
    public void proveedorTieneRecurso(String nombre) {
        recursoId = datos.crearRecursoDeProveedor(usuarioId, nombre);
    }

    @Dado("que el proveedor tiene el servicio {string} registrado")
    public void proveedorTieneServicio(String nombre) {
        serviciosPorNombre.put(nombre, datos.crearServicioDeUsuario(usuarioId, nombre));
    }

    @Dado("que existe otro proveedor {string} con correo {string} que tiene el servicio {string}")
    public void otroProveedorTieneServicio(String proveedor, String email, String servicio) {
        Integer otroUsuarioId = datos.crearProveedor(proveedor, email);
        serviciosPorNombre.put(servicio, datos.crearServicioDeUsuario(otroUsuarioId, servicio));
    }

    @Dado("que el servicio {string} tiene una reserva {string}")
    public void servicioTieneReserva(String servicio, String estado) {
        datos.crearReservaDeServicio(serviciosPorNombre.get(servicio), "Cliente reservante", estado);
    }

    @Cuando("elimino el servicio {string}")
    public void eliminoServicio(String nombre) {
        api.delete(RUTA_SERVICIOS + "/" + serviciosPorNombre.get(nombre));
    }

    @Cuando("intento eliminar el servicio inexistente {int}")
    public void eliminoServicioInexistente(int servicioId) {
        api.delete(RUTA_SERVICIOS + "/" + servicioId);
    }

    @Cuando("intento eliminar el servicio {string} sin iniciar sesión")
    public void eliminoServicioSinSesion(String nombre) {
        api.sinSesion();
        eliminoServicio(nombre);
    }

    @Entonces("el servicio {string} está {string}")
    public void servicioEsta(String nombre, String estado) {
        assertThat(estado).isIn("activo", "eliminado");
        assertThat(datos.servicioEliminado(serviciosPorNombre.get(nombre)))
            .isEqualTo("eliminado".equals(estado));
    }

    @Entonces("la reserva de {string} sigue en estado {string}")
    public void reservaSigueEnEstado(String nombre, String estado) {
        assertThat(datos.estadoDeReserva(nombre)).isEqualTo(estado);
    }

    @Entonces("el proveedor autenticado tiene {int} servicios activos")
    public void proveedorTieneServiciosActivos(int total) {
        assertThat(datos.contarServiciosActivosDeUsuario(usuarioId)).isEqualTo(total);
    }

    @Entonces("el servicio {string} no aparece en el catálogo")
    public void servicioNoApareceEnCatalogo(String nombre) {
        Integer idServicio = serviciosPorNombre.get(nombre);
        api.get("/api/v1/providers/" + datos.proveedorDeServicio(idServicio) + "/services");
        assertThat(api.ultimaRespuesta().statusCode()).isEqualTo(200);
        JsonNode servicios = api.ultimoJson().get("data");
        for (JsonNode servicio : servicios) {
            assertThat(servicio.get("id").asInt()).isNotEqualTo(idServicio);
        }
    }

    @Entonces("la disponibilidad del servicio {string} no se puede consultar")
    public void disponibilidadServicioEliminado(String nombre) {
        api.get("/api/v1/services/" + serviciosPorNombre.get(nombre) + "/availability");
        assertThat(api.ultimaRespuesta().statusCode()).isEqualTo(404);
        assertThat(api.ultimoJson().get("errorCode").asString()).isEqualTo("SERVICE_NOT_FOUND");
    }

    @Entonces("el servicio {string} no admite nuevas reservas")
    public void servicioEliminadoNoAdmiteReservas(String nombre) {
        api.iniciarSesion(
            "Cliente reservante",
            datos.emailDeUsuario("Cliente reservante"),
            DatosPrueba.CONTRASENA_POR_DEFECTO
        );
        assertThat(api.ultimaRespuesta().statusCode()).isEqualTo(200);
        api.postJson(
            "/api/reservas",
            Map.of(
                "servicioId", serviciosPorNombre.get(nombre),
                "fecha", datos.hoy().plusDays(7).toString(),
                "hora", "09:00:00"
            )
        );
        assertThat(api.ultimaRespuesta().statusCode()).isEqualTo(404);
    }

    @Dado("que existe el cliente {string} con correo {string}")
    public void existeCliente(String nombre, String email) {
        usuarioId = datos.crearUsuario(
            nombre,
            email,
            DatosPrueba.CONTRASENA_POR_DEFECTO,
            "usuario"
        );
        api.iniciarSesion(nombre, email, DatosPrueba.CONTRASENA_POR_DEFECTO);
    }

    @Dado("que existe una cuenta proveedor sin perfil con correo {string}")
    public void existeProveedorSinPerfil(String email) {
        usuarioId = datos.crearUsuario(
            "Proveedor sin perfil",
            email,
            DatosPrueba.CONTRASENA_POR_DEFECTO,
            "proveedor"
        );
        api.iniciarSesion("sin-perfil", email, DatosPrueba.CONTRASENA_POR_DEFECTO);
    }

    @Dado("que existe una cuenta proveedor con perfiles duplicados y correo {string}")
    public void existeProveedorConPerfilesDuplicados(String email) {
        usuarioId = datos.crearUsuario(
            "Proveedor duplicado",
            email,
            DatosPrueba.CONTRASENA_POR_DEFECTO,
            "proveedor"
        );
        datos.crearPerfilProveedor(usuarioId, "Negocio uno");
        datos.crearPerfilProveedor(usuarioId, "Negocio dos");
        api.iniciarSesion("perfiles-duplicados", email, DatosPrueba.CONTRASENA_POR_DEFECTO);
    }

    @Cuando("creo un servicio con nombre {string}, descripción {string}, duración {int} minutos")
    public void creoServicio(String nombre, String descripcion, int duracion) {
        api.postJson(
            RUTA_SERVICIOS,
            Map.of(
                "nombre",
                nombre,
                "descripcion",
                descripcion,
                "duracion",
                duracion,
                "recursoIds",
                List.of(recursoId)
            )
        );
    }

    @Cuando("creo un servicio usando ese recurso")
    public void creoServicioUsandoRecurso() {
        api.postJson(
            RUTA_SERVICIOS,
            Map.of(
                "nombre",
                "Consulta",
                "descripcion",
                "Consulta general",
                "duracion",
                30,
                "recursoIds",
                List.of(recursoId)
            )
        );
    }

    @Cuando("creo dos servicios con el mismo nombre {string}")
    public void creoDosServiciosConElMismoNombre(String nombre) {
        Map<String, Object> cuerpo = Map.of(
            "nombre",
            nombre,
            "descripcion",
            "Descripción de prueba",
            "duracion",
            30,
            "recursoIds",
            List.of(recursoId)
        );
        api.postJson(RUTA_SERVICIOS, cuerpo);
        api.postJson(RUTA_SERVICIOS, cuerpo);
    }

    @Cuando("intento crear un servicio")
    public void intentoCrearServicio() {
        api.postJson(
            RUTA_SERVICIOS,
            Map.of(
                "nombre",
                "Servicio de prueba",
                "descripcion",
                "Descripción de prueba",
                "duracion",
                30,
                "recursoIds",
                RECURSOS_DE_RELLENO
            )
        );
    }

    @Cuando("intento crear un servicio sin iniciar sesión")
    public void intentoCrearServicioSinSesion() {
        api.sinSesion();
        intentoCrearServicio();
    }

    @Cuando("intento crear un servicio sin token CSRF")
    public void intentoCrearServicioSinCsrf() {
        api.postJsonSinCsrf(
            RUTA_SERVICIOS,
            Map.of(
                "nombre",
                "Corte",
                "descripcion",
                "Descripción",
                "duracion",
                30,
                "recursoIds",
                RECURSOS_DE_RELLENO
            )
        );
    }

    @Cuando("creo un servicio con campos vacíos")
    public void creoServicioConCamposVacios() {
        api.postJson(
            RUTA_SERVICIOS,
            Map.of("nombre", " ", "descripcion", "", "duracion", 30)
        );
    }

    @Entonces("el servicio {string} queda guardado para el proveedor autenticado")
    public void servicioGuardadoParaProveedorAutenticado(String nombre) {
        assertThat(api.ultimoJson().get("data").get("nombre").asString()).isEqualTo(nombre);
        assertThat(datos.contarServiciosDeUsuario(usuarioId)).isEqualTo(1);
    }

    @Entonces("el recurso queda asociado al servicio creado")
    public void recursoAsociadoAlServicio() {
        JsonNode data = api.ultimoJson().get("data");
        Integer idServicio = data.get("id").asInt();
        assertThat(data.get("recursoIds").get(0).asInt()).isEqualTo(recursoId);
        assertThat(datos.contarRecursosCompatibles(idServicio)).isEqualTo(1);
    }

    @Y("el proveedor autenticado tiene {int} servicios guardados")
    public void proveedorAutenticadoTieneServicios(int total) {
        assertThat(datos.contarServiciosDeUsuario(usuarioId)).isEqualTo(total);
    }

    @Y("existen {int} servicios guardados")
    public void existenServiciosGuardados(int total) {
        assertThat(datos.contarServiciosDeUsuario(usuarioId == null ? -1 : usuarioId))
            .isEqualTo(total);
    }
}
