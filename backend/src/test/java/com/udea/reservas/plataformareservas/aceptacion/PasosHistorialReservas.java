package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.time.LocalTime;
import java.util.Objects;
import tools.jackson.databind.JsonNode;

public class PasosHistorialReservas {

    private static final String RUTA_HISTORIAL = "/api/reservas/historial";

    private final ClienteApi api;
    private final DatosPrueba datos;

    public PasosHistorialReservas(ClienteApi api, DatosPrueba datos) {
        this.api = api;
        this.datos = datos;
    }

    @Dado("que existe el usuario {string}")
    public void existeElUsuario(String nombre) {
        datos.crearUsuario(nombre);
    }

    @Y("{string} tiene una reserva en el historial a las {string} con estado {string}")
    public void tieneReservaEnHistorial(String nombre, String hora, String estado) {
        datos.crearReserva(nombre, LocalTime.parse(hora), estado);
    }

    @Cuando("{string} consulta su historial de reservas")
    public void consultaSuHistorial(String nombre) {
        autenticarComo(nombre);
        api.get(RUTA_HISTORIAL);
    }

    @Cuando("{string} consulta el detalle de esa reserva")
    public void consultaDetalleDeReserva(String nombre) {
        autenticarComo(nombre);
        api.get("/api/reservas/" + datos.idDeReserva(nombre) + "/detalle");
    }

    @Entonces("la lista del historial contiene {int} reservas")
    public void listaHistorialContiene(int cantidad) {
        assertThat(api.ultimoJson()).hasSize(cantidad);
    }

    @Y("la reserva de las {string} muestra el servicio {string}, la fecha del horario y el estado {string}")
    public void reservaMuestraServicioFechaYEstado(String hora, String servicio, String estado) {
        JsonNode reserva = buscarReserva(hora);
        assertThat(reserva).as("Reserva de las %s", hora).isNotNull();
        assertThat(reserva.get("servicioNombre").asString()).isEqualTo(servicio);
        assertThat(reserva.get("fecha").asString()).isEqualTo(datos.fechaHorario().toString());
        assertThat(reserva.get("estado").asString()).isEqualTo(estado);
    }

    @Y("la reserva de las {string} aparece con estado {string}")
    public void reservaApareceConEstado(String hora, String estado) {
        JsonNode reserva = buscarReserva(hora);
        assertThat(reserva).as("Reserva de las %s", hora).isNotNull();
        assertThat(reserva.get("estado").asString()).isEqualTo(estado);
    }

    @Y("veo un mensaje indicando que aún no tengo reservas")
    public void veoMensajeSinReservas() {
        JsonNode respuesta = api.ultimoJson();
        assertThat(respuesta.isObject()).isTrue();
        assertThat(mensajeDe(respuesta))
            .as("Mensaje del historial sin reservas")
            .isNotNull()
            .asString()
            .containsIgnoringCase("aún no tengo reservas");
    }

    private JsonNode mensajeDe(JsonNode respuesta) {
        if (respuesta.has("message")) {
            return respuesta.get("message");
        }
        return respuesta.get("mensaje");
    }

    @Y("el detalle muestra el servicio {string}, la fecha del horario, el horario {string}, el estado {string} y el usuario {string}")
    public void detalleMuestraDatos(
        String servicio,
        String hora,
        String estado,
        String usuario
    ) {
        JsonNode detalle = api.ultimoJson();
        assertThat(detalle.get("servicioNombre").asString()).isEqualTo(servicio);
        assertThat(detalle.get("fecha").asString()).isEqualTo(datos.fechaHorario().toString());
        assertThat(detalle.get("hora").asString()).startsWith(hora);
        assertThat(detalle.get("estado").asString()).isEqualTo(estado);
        assertThat(detalle.get("nombreUsuario").asString()).isEqualTo(usuario);
        assertThat(detalle.get("emailUsuario").asString())
            .isEqualTo(datos.emailDeUsuario(usuario));
    }

    private JsonNode buscarReserva(String hora) {
        for (JsonNode reserva : api.ultimoJson()) {
            if (Objects.equals(hora, reserva.get("hora").asString())) {
                return reserva;
            }
        }
        return null;
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
}
