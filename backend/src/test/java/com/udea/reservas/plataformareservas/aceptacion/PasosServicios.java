package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasosServicios {

    private static final String RUTA_SERVICIOS = "/api/v1/providers/me/services";

    private final ClienteApi api;
    private final DatosPrueba datos;

    private Integer usuarioId;
    private final Map<String, Integer> serviciosPorNombre = new HashMap<>();

    @Dado("que existe el proveedor {string} con correo {string}")
    public void existeProveedor(String nombre, String email) {
        usuarioId = datos.crearProveedor(nombre, email);
        api.iniciarSesion(nombre, email, DatosPrueba.CONTRASENA_POR_DEFECTO);
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
            Map.of("nombre", nombre, "descripcion", descripcion, "duracion", duracion)
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
            30
        );
        api.postJson(RUTA_SERVICIOS, cuerpo);
        api.postJson(RUTA_SERVICIOS, cuerpo);
    }

    @Cuando("intento crear un servicio")
    public void intentoCrearServicio() {
        creoServicio("Servicio de prueba", "Descripción de prueba", 30);
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
            Map.of("nombre", "Corte", "descripcion", "Descripción", "duracion", 30)
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

    @Dado("que el proveedor tiene el servicio {string} registrado")
    public void proveedorTieneServicio(String nombre) {
        serviciosPorNombre.put(nombre, datos.crearServicio(usuarioId, nombre));
    }

    @Dado("que el servicio {string} tiene una reserva {string}")
    public void servicioTieneReserva(String nombre, String estado) {
        datos.crearReservaDeServicio(serviciosPorNombre.get(nombre), "Cliente reservante", estado);
    }

    @Dado("que existe otro proveedor {string} con correo {string} que tiene el servicio {string}")
    public void existeOtroProveedorConServicio(String nombre, String email, String servicio) {
        Integer otroUsuarioId = datos.crearProveedor(nombre, email);
        serviciosPorNombre.put(servicio, datos.crearServicio(otroUsuarioId, servicio));
    }

    @Cuando("elimino el servicio {string}")
    public void eliminoServicio(String nombre) {
        api.delete(RUTA_SERVICIOS + "/" + serviciosPorNombre.get(nombre));
    }

    @Cuando("intento eliminar el servicio inexistente {int}")
    public void intentoEliminarServicioInexistente(int id) {
        api.delete(RUTA_SERVICIOS + "/" + id);
    }

    @Cuando("intento eliminar el servicio {string} sin iniciar sesión")
    public void intentoEliminarServicioSinSesion(String nombre) {
        api.sinSesion();
        eliminoServicio(nombre);
    }

    @Entonces("el servicio {string} está {string}")
    public void servicioEstaEnEstado(String nombre, String estado) {
        assertThat(datos.servicioActivo(serviciosPorNombre.get(nombre)))
            .isEqualTo("activo".equals(estado));
    }

    @Y("el proveedor autenticado tiene {int} servicios activos")
    public void proveedorAutenticadoTieneServiciosActivos(int total) {
        assertThat(datos.contarServiciosActivosDeUsuario(usuarioId)).isEqualTo(total);
    }

    @Y("la reserva de {string} sigue en estado {string}")
    public void reservaSigueEnEstado(String cliente, String estado) {
        assertThat(datos.estadoDeReserva(cliente)).isEqualTo(estado);
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
