package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@RequiredArgsConstructor
public class PasosReservas {

    private static final String RUTA_RESERVAS = "/api/reservas";

    private final ClienteApi api;
    private final DatosPrueba datos;

    private final List<Integer> codigosSimultaneos = new ArrayList<>();

    @Dado("que existe un horario habilitado de {string} a {string}")
    public void existeHorarioHabilitado(String inicio, String fin) {
        datos.crearHorarioHabilitado(LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    @Y("el proveedor deshabilitó su agenda")
    public void proveedorDeshabilitoAgenda() {
        datos.desactivarAgenda();
    }

    @Y("existe el usuario {string}")
    public void existeElUsuario(String nombre) {
        datos.crearUsuario(nombre);
    }

    @Cuando("{string} reserva ese horario a las {string}")
    public void reservaEseHorario(String nombre, String hora) {
        autenticarComo(nombre);
        reservar(datos.horarioId(), datos.fechaHorario(), hora);
        guardarReservaCreada(nombre);
    }

    @Cuando("{string} reserva ese horario para ayer a las {string}")
    public void reservaParaAyer(String nombre, String hora) {
        autenticarComo(nombre);
        reservar(datos.horarioId(), datos.hoy().minusDays(1), hora);
    }

    @Cuando("{string} reserva ese horario un día de la semana distinto a las {string}")
    public void reservaOtroDia(String nombre, String hora) {
        autenticarComo(nombre);
        reservar(datos.horarioId(), datos.fechaHorario().plusDays(1), hora);
    }

    @Dado("que {string} inició sesión")
    public void inicioSesion(String nombre) {
        autenticarComo(nombre);
    }

    @Y("la cuenta de {string} fue eliminada")
    public void laCuentaFueEliminada(String nombre) {
        datos.eliminarUsuario(nombre);
    }

    @Cuando("{string} reserva el horario inexistente {int} a las {string}")
    public void reservaHorarioInexistente(String nombre, int horarioId, String hora) {
        autenticarComo(nombre);
        reservar(horarioId, datos.fechaHorario(), hora);
    }

    @Cuando("envío una solicitud de reserva sin datos")
    public void envioReservaSinDatos() {
        autenticarComo("Ana");
        api.postJson(RUTA_RESERVAS, Map.of());
    }

    @Cuando("intento reservar ese horario a las {string} sin haber iniciado sesión")
    public void reservarSinSesion(String hora) {
        api.sinSesion();
        reservar(datos.horarioId(), datos.fechaHorario(), hora);
    }

    @Cuando("{string} y {string} reservan ese horario al mismo tiempo a las {string}")
    public void reservanAlMismoTiempo(String primero, String segundo, String hora) {
        autenticarComo(primero);
        autenticarComo(segundo);
        Map<String, Object> cuerpos = new LinkedHashMap<>();
        cuerpos.put(primero, cuerpoReserva(datos.horarioId(), datos.fechaHorario(), hora));
        cuerpos.put(segundo, cuerpoReserva(datos.horarioId(), datos.fechaHorario(), hora));
        api.postJsonSimultaneos(RUTA_RESERVAS, cuerpos)
            .forEach(respuesta -> codigosSimultaneos.add(respuesta.statusCode()));
    }

    @Entonces("una solicitud responde {int} y la otra responde {int}")
    public void unaRespondeYOtraResponde(int codigoA, int codigoB) {
        assertThat(codigosSimultaneos).containsExactlyInAnyOrder(codigoA, codigoB);
    }

    @Y("la reserva queda activa para ese horario y fecha")
    public void laReservaQuedaActiva() {
        JsonNode json = api.ultimoJson();
        assertThat(json.get("estado").asString()).isEqualTo("activa");
        assertThat(json.get("horarioId").asInt()).isEqualTo(datos.horarioId());
        assertThat(LocalDate.parse(json.get("fecha").asString())).isEqualTo(datos.fechaHorario());
    }

    @Y("el horario tiene {int} reserva(s) activa(s) en esa fecha")
    public void elHorarioTieneReservasActivas(int total) {
        assertThat(datos.contarReservasActivasDelHorario()).isEqualTo(total);
    }

    @Dado("que {string} tiene una reserva activa en ese horario a las {string}")
    public void tieneUnaReservaActiva(String nombre, String hora) {
        datos.crearReserva(nombre, LocalTime.parse(hora), "activa");
    }

    @Y("la reserva de {string} está {string}")
    public void laReservaEsta(String nombre, String estado) {
        datos.cambiarEstadoReserva(nombre, estado);
    }

    @Cuando("{string} cancela su reserva")
    public void cancelaSuReserva(String nombre) {
        autenticarComo(nombre);
        cancelar(datos.idDeReserva(nombre), "");
    }

    @Cuando("{string} cancela la reserva de {string}")
    public void cancelaReservaAjena(String nombre, String duenio) {
        autenticarComo(nombre);
        cancelar(datos.idDeReserva(duenio), "");
    }

    @Cuando("{string} cancela la reserva de {string} enviando el identificador de {string} en la solicitud")
    public void cancelaReservaAjenaSuplantando(String nombre, String duenio, String suplantado) {
        autenticarComo(nombre);
        cancelar(datos.idDeReserva(duenio), "?usuarioId=" + datos.idDeUsuario(suplantado));
    }

    @Cuando("{string} cancela la reserva inexistente {int}")
    public void cancelaReservaInexistente(String nombre, int reservaId) {
        autenticarComo(nombre);
        cancelar(reservaId, "");
    }

    @Cuando("se intenta cancelar la reserva de {string} sin haber iniciado sesión")
    public void cancelarSinSesion(String nombre) {
        api.sinSesion();
        cancelar(datos.idDeReserva(nombre), "");
    }

    @Entonces("la reserva de {string} queda en estado {string}")
    public void laReservaQuedaEnEstado(String nombre, String estado) {
        assertThat(datos.estadoDeReserva(nombre)).isEqualTo(estado);
    }

    /** Cada persona usa su propia sesión; se inicia la primera vez que actúa. */
    private void autenticarComo(String nombre) {
        if (!api.haySesion(nombre)) {
            api.iniciarSesion(nombre, datos.emailDeUsuario(nombre), DatosPrueba.CONTRASENA_POR_DEFECTO);
        }
        api.usarSesion(nombre);
    }

    private void reservar(Integer horarioId, LocalDate fecha, String hora) {
        api.postJson(RUTA_RESERVAS, cuerpoReserva(horarioId, fecha, hora));
    }

    private static Map<String, Object> cuerpoReserva(Integer horarioId, LocalDate fecha, String hora) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("horarioId", horarioId);
        cuerpo.put("fecha", fecha.toString());
        cuerpo.put("hora", hora);
        return cuerpo;
    }

    private void guardarReservaCreada(String nombre) {
        HttpResponse<String> respuesta = api.ultimaRespuesta();
        if (respuesta.statusCode() == 201) {
            datos.registrarReserva(nombre, api.ultimoJson().get("id").asInt());
        }
    }

    private void cancelar(Integer reservaId, String parametros) {
        api.delete(RUTA_RESERVAS + "/" + reservaId + "/cancelar" + parametros);
    }
}
