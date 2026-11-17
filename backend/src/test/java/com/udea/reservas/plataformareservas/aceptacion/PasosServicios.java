package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final Map<String, Integer> proveedoresPorNombre = new HashMap<>();

    @Dado("que existe el proveedor {string} con correo {string}")
    public void existeProveedor(String nombre, String email) {
        usuarioId = datos.crearProveedor(nombre, email);
        proveedoresPorNombre.put(nombre, datos.proveedorDeUsuario(usuarioId));
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
        proveedoresPorNombre.put(proveedor, datos.proveedorDeUsuario(otroUsuarioId));
        serviciosPorNombre.put(servicio, datos.crearServicioDeUsuario(otroUsuarioId, servicio));
    }

    @Dado("que estoy autenticado como usuario")
    public void estoyAutenticadoComoUsuario() {
        datos.crearUsuario(
            "Usuario catálogo",
            "catalogo@test.com",
            DatosPrueba.CONTRASENA_POR_DEFECTO,
            "usuario"
        );
        api.iniciarSesion(
            "Usuario catálogo",
            "catalogo@test.com",
            DatosPrueba.CONTRASENA_POR_DEFECTO
        );
    }

    @Dado("que hay {int} proveedores registrados")
    public void hayProveedoresRegistrados(int cantidad) {
        for (int indice = 1; indice <= cantidad; indice++) {
            String nombre = "Proveedor catálogo " + indice;
            Integer id = datos.crearProveedor(
                nombre,
                "catalogo-" + indice + "@test.com"
            );
            proveedoresPorNombre.put(nombre, datos.proveedorDeUsuario(id));
        }
    }

    @Dado("que el proveedor {string} no tiene servicios registrados")
    public void proveedorNoTieneServicios(String nombre) {
        Integer id = datos.crearProveedor(nombre, nombre.toLowerCase().replace(' ', '-') + "@test.com");
        proveedoresPorNombre.put(nombre, datos.proveedorDeUsuario(id));
    }

    @Dado("que el proveedor {string} ofrece el servicio {string}")
    public void proveedorOfreceServicio(String proveedor, String servicio) {
        Integer proveedorId = proveedoresPorNombre.get(proveedor);
        if (proveedorId == null) {
            Integer nuevoUsuarioId = datos.crearProveedor(
                proveedor,
                proveedor.toLowerCase().replace(' ', '-') + "@test.com"
            );
            proveedorId = datos.proveedorDeUsuario(nuevoUsuarioId);
            proveedoresPorNombre.put(proveedor, proveedorId);
        }
        Integer usuarioProveedorId = datos.usuarioDeProveedor(proveedorId);
        serviciosPorNombre.put(
            servicio,
            datos.crearServicioDeUsuario(usuarioProveedorId, servicio)
        );
    }

    @Dado("que el servicio {string} tiene horarios habilitados")
    public void servicioTieneHorariosHabilitados(String servicio) {
        datos.crearAgendaYHorarioParaServicio(serviciosPorNombre.get(servicio));
    }

    @Dado("que el servicio {string} tiene un horario que ya pasó")
    public void servicioTieneHorarioPasado(String servicio) {
        datos.crearHorarioPasadoParaServicio();
    }

    @Dado("que uno de los horarios del servicio {string} ya está reservado")
    public void horarioServicioYaEstaReservado(String servicio) {
        datos.crearUsuario(
            "Usuario con reserva",
            "reserva-catalogo@test.com",
            DatosPrueba.CONTRASENA_POR_DEFECTO,
            "usuario"
        );
        datos.crearReservaEnFecha(
            "Usuario con reserva",
            datos.fechaHorario(),
            LocalTime.of(8, 0),
            "activa"
        );
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

    @Cuando("consulto mis servicios")
    public void consultoMisServicios() {
        api.get(RUTA_SERVICIOS);
    }

    @Cuando("accedo a la sección de servicios")
    public void accedoASeccionDeServicios() {
        api.get("/api/v1/providers?page=0&size=1");
    }

    @Cuando("avanzo a la siguiente página de proveedores")
    public void avanzoSiguientePaginaProveedores() {
        api.get("/api/v1/providers?page=1&size=1");
    }

    @Cuando("selecciono el proveedor {string}")
    public void seleccionoProveedor(String nombre) {
        api.get("/api/v1/providers/" + proveedoresPorNombre.get(nombre) + "/services");
    }

    @Cuando("selecciono el servicio {string}")
    public void seleccionoServicio(String nombre) {
        api.get("/api/v1/services/" + serviciosPorNombre.get(nombre) + "/availability");
    }

    @Entonces("veo {int} proveedores en la página")
    public void veoProveedoresEnPagina(int cantidad) {
        assertThat(api.ultimoJson().get("data").get("content")).hasSize(cantidad);
    }

    @Y("la paginación indica que hay más de una página")
    public void paginacionIndicaMasDeUnaPagina() {
        assertThat(api.ultimoJson().get("data").get("totalPages").asInt()).isGreaterThan(1);
    }

    @Y("el proveedor {string} aparece en la lista")
    public void proveedorApareceEnLista(String nombre) {
        assertThat(api.ultimoJson().get("data").get("content"))
            .anyMatch(proveedor -> nombre.equals(proveedor.get("nombre").asString()));
    }

    @Entonces("veo {int} servicios del proveedor")
    public void veoServiciosDelProveedor(int cantidad) {
        assertThat(api.ultimoJson().get("data")).hasSize(cantidad);
    }

    @Y("la respuesta indica que el proveedor no tiene servicios disponibles")
    public void respuestaIndicaProveedorSinServicios() {
        assertThat(mensajeRespuesta())
            .containsIgnoringCase("no tiene servicios disponibles");
    }

    @Y("la respuesta indica que no hay proveedores disponibles")
    public void respuestaIndicaSinProveedores() {
        assertThat(mensajeRespuesta())
            .containsIgnoringCase("no hay proveedores disponibles");
    }

    @Y("el servicio {string} muestra duración {int} minutos y proveedor {string}")
    public void servicioMuestraDatos(String servicio, int duracion, String proveedor) {
        JsonNode encontrado = null;
        for (JsonNode item : api.ultimoJson().get("data")) {
            if (servicio.equals(item.get("nombre").asString())) {
                encontrado = item;
                break;
            }
        }
        if (encontrado == null) {
            throw new AssertionError("El servicio '" + servicio + "' no aparece en la respuesta");
        }
        assertThat(encontrado.get("duracionMinutos").asInt()).isEqualTo(duracion);
        assertThat(encontrado.get("proveedorNombre").asString()).isEqualTo(proveedor + " negocio");
    }

    @Entonces("veo horarios futuros y libres del servicio {string}")
    public void veoHorariosFuturosYLibres(String servicio) {
        JsonNode horarios = api.ultimoJson().get("data");
        assertThat(horarios).isNotEmpty();
        LocalDateTime ahora = LocalDateTime.now();
        for (JsonNode horario : horarios) {
            LocalDate fecha = LocalDate.parse(horario.get("fecha").asString());
            LocalTime hora = LocalTime.parse(horario.get("hora").asString());
            assertThat(LocalDateTime.of(fecha, hora)).isAfter(ahora);
            if (fecha.equals(datos.fechaHorario())) {
                assertThat(hora).isNotEqualTo(LocalTime.of(8, 0));
            }
        }
    }

    @Y("el proveedor {string} no aparece como disponible")
    public void proveedorNoApareceComoDisponible(String nombre) {
        assertThat(api.ultimoJson().get("data").get("content"))
            .noneMatch(proveedor -> nombre.equals(proveedor.get("nombreNegocio").asString()));
    }

    @Cuando("intento consultar mis servicios sin iniciar sesión")
    public void consultoMisServiciosSinSesion() {
        api.sinSesion();
        consultoMisServicios();
    }

    @Entonces("la lista contiene {int} servicios")
    public void listaContieneServicios(int cantidad) {
        assertThat(api.ultimoJson().get("data")).hasSize(cantidad);
    }

    @Y("el servicio {string} aparece en la lista con duración {int} minutos y estado {string}")
    public void servicioApareceEnLista(String nombre, int duracion, String estado) {
        JsonNode servicio = buscarEnLista(nombre);
        assertThat(servicio).as("Servicio '%s' en la lista", nombre).isNotNull();
        assertThat(servicio.get("duracionMinutos").asInt()).isEqualTo(duracion);
        assertThat(servicio.get("estado").asString()).isEqualTo(estado);
    }

    @Y("el servicio {string} no aparece en la lista")
    public void servicioNoApareceEnLista(String nombre) {
        assertThat(buscarEnLista(nombre)).as("Servicio '%s' en la lista", nombre).isNull();
    }

    private JsonNode buscarEnLista(String nombre) {
        for (JsonNode servicio : api.ultimoJson().get("data")) {
            if (nombre.equals(servicio.get("nombre").asString())) {
                return servicio;
            }
        }
        return null;
    }

    private String mensajeRespuesta() {
        JsonNode respuesta = api.ultimoJson();
        assertThat(mensajeDe(respuesta)).as("Mensaje de respuesta").isNotNull();
        return mensajeDe(respuesta).asString();
    }

    private JsonNode mensajeDe(JsonNode respuesta) {
        if (respuesta.has("message")) {
            return respuesta.get("message");
        }
        return respuesta.get("mensaje");
    }

    @Y("el mensaje indica que el campo {string} es obligatorio")
    public void mensajeIndicaCampoObligatorio(String campo) {
        assertThat(api.ultimaRespuesta().body())
            .containsIgnoringCase(campo)
            .containsIgnoringCase("obligatorio");
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
        if (api.ultimaRespuesta().statusCode() == 201) {
            serviciosPorNombre.put(nombre, api.ultimoJson().get("data").get("id").asInt());
        }
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

    @Cuando("intento crear un servicio dejando el campo {string} sin completar")
    public void intentoCrearServicioConCampoVacio(String campo) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("nombre", "Servicio de prueba");
        cuerpo.put("descripcion", "Descripción de prueba");
        cuerpo.put("duracion", 30);
        List<Integer> recursos = RECURSOS_DE_RELLENO;
        if (recursoId != null) {
            recursos = List.of(recursoId);
        }
        cuerpo.put("recursoIds", recursos);

        if ("nombre".equals(campo) || "descripcion".equals(campo)) {
            cuerpo.put(campo, " ");
        } else {
            cuerpo.remove(campo);
        }
        api.postJson(RUTA_SERVICIOS, cuerpo);
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

    @Y("el servicio {string} aparece en el catálogo para los usuarios")
    public void servicioApareceEnCatalogo(String nombre) {
        Integer servicioId = serviciosPorNombre.get(nombre);
        api.get("/api/v1/providers/" + datos.proveedorDeServicio(servicioId) + "/services");
        assertThat(api.ultimaRespuesta().statusCode()).isEqualTo(200);
        JsonNode servicios = api.ultimoJson().get("data");
        assertThat(servicios).anyMatch(servicio -> nombre.equals(servicio.get("nombre").asString()));
    }

    @Y("existen {int} servicios guardados")
    public void existenServiciosGuardados(int total) {
        int idUsuario = -1;
        if (usuarioId != null) {
            idUsuario = usuarioId;
        }
        assertThat(datos.contarServiciosDeUsuario(idUsuario)).isEqualTo(total);
    }
}
