package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import tools.jackson.databind.JsonNode;

public class PasosReservasActivas {

    private static final String RUTA_RESERVAS_ACTIVAS = "/api/v1/reservations/me/active";

    private final ClienteApi api;
    private final DatosPrueba datos;

    public PasosReservasActivas(ClienteApi api, DatosPrueba datos) {
        this.api = api;
        this.datos = datos;
    }

    @Cuando("{string} consulta sus reservas activas")
    public void consultaSusReservasActivas(String nombre) {
        autenticarComo(nombre);
        api.get(RUTA_RESERVAS_ACTIVAS);
    }

    @Cuando("{string} consulta sus reservas activas enviando el identificador de {string}")
    public void consultaReservasActivasConUsuarioAjeno(String nombre, String otroUsuario) {
        autenticarComo(nombre);
        api.get(RUTA_RESERVAS_ACTIVAS + "?userId=" + datos.idDeUsuario(otroUsuario));
    }

    @Cuando("consulto las reservas activas sin iniciar sesión")
    public void consultaReservasActivasSinSesion() {
        api.sinSesion();
        api.get(RUTA_RESERVAS_ACTIVAS);
    }

    private void autenticarComo(String nombre) {
        if (!api.haySesion(nombre)) {
            api.iniciarSesion(
                nombre,
                datos.emailDeUsuario(nombre),
                DatosPrueba.CONTRASENA_POR_DEFECTO
            );
        }
        api.usarSesion(nombre);
    }

    @Entonces("la lista de reservas activas contiene {int} reservas")
    public void listaReservasActivasContiene(int cantidad) {
        JsonNode respuesta = api.ultimoJson();
        JsonNode reservas = respuesta.isObject() ? respuesta.get("data") : respuesta;
        assertThat(reservas).isNotNull();
        assertThat(reservas.isArray()).isTrue();
        assertThat(reservas).hasSize(cantidad);
    }

    @Y("la reserva de las {string} muestra el servicio {string}, la fecha del horario, el horario y el estado {string}")
    public void reservaActivaMuestraDatos(String hora, String servicio, String estado) {
        JsonNode reserva = buscarReserva(hora);
        assertThat(reserva).as("Reserva activa de las %s", hora).isNotNull();
        assertThat(reserva.get("serviceName").asString()).isEqualTo(servicio);
        assertThat(reserva.get("date").asString()).isEqualTo(datos.fechaHorario().toString());
        assertThat(reserva.get("startTime").asString()).startsWith(hora);
        assertThat(reserva.get("status").asString()).isEqualTo(estado);
    }

    @Y("veo un mensaje indicando que aún no tengo reservas activas")
    public void veoMensajeSinReservasActivas() {
        JsonNode respuesta = api.ultimoJson();
        assertThat(respuesta.isObject()).isTrue();
        JsonNode mensaje = respuesta.get("message");
        assertThat(mensaje).as("Mensaje de reservas activas vacías").isNotNull();
        assertThat(mensaje.asString()).isEqualTo("Aún no tienes reservas activas.");
    }

    private JsonNode buscarReserva(String hora) {
        for (JsonNode reserva : api.ultimoJson()) {
            if (reserva.get("startTime").asString().startsWith(hora)) {
                return reserva;
            }
        }
        return null;
    }
}
