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
        reservar(datos.idDeUsuario(nombre), datos.horarioId(), datos.fechaHorario(), hora);
        guardarReservaCreada(nombre);
    }

    @Cuando("{string} reserva ese horario para ayer a las {string}")
    public void reservaParaAyer(String nombre, String hora) {
        LocalDate ayer = datos.hoy().minusDays(1);
        reservar(datos.idDeUsuario(nombre), datos.horarioId(), ayer, hora);
    }

    @Cuando("{string} reserva ese horario un día de la semana distinto a las {string}")
    public void reservaOtroDia(String nombre, String hora) {
        reservar(datos.idDeUsuario(nombre), datos.horarioId(), datos.fechaHorario().plusDays(1), hora);
    }

    @Cuando("un usuario inexistente reserva ese horario a las {string}")
    public void usuarioInexistenteReserva(String hora) {
        reservar(999_999, datos.horarioId(), datos.fechaHorario(), hora);
    }

    @Cuando("{string} reserva el horario inexistente {int} a las {string}")
    public void reservaHorarioInexistente(String nombre, int horarioId, String hora) {
        reservar(datos.idDeUsuario(nombre), horarioId, datos.fechaHorario(), hora);
    }

    @Cuando("envío una solicitud de reserva sin datos")
    public void envioReservaSinDatos() {
        api.postJson(RUTA_RESERVAS, Map.of());
    }

    @Cuando("{string} y {string} reservan ese horario al mismo tiempo a las {string}")
    public void reservanAlMismoTiempo(String primero, String segundo, String hora) {
        List<Map<String, Object>> cuerpos = List.of(
            cuerpoReserva(datos.idDeUsuario(primero), datos.horarioId(), datos.fechaHorario(), hora),
            cuerpoReserva(datos.idDeUsuario(segundo), datos.horarioId(), datos.fechaHorario(), hora)
        );
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
        cancelar(datos.idDeReserva(nombre), datos.idDeUsuario(nombre));
    }

    @Cuando("{string} cancela la reserva de {string} indicando su propio identificador")
    public void cancelaReservaAjena(String nombre, String duenio) {
        cancelar(datos.idDeReserva(duenio), datos.idDeUsuario(nombre));
    }

    @Cuando("{string} cancela la reserva inexistente {int}")
    public void cancelaReservaInexistente(String nombre, int reservaId) {
        cancelar(reservaId, datos.idDeUsuario(nombre));
    }

    @Cuando("se intenta cancelar la reserva de {string} sin indicar el usuario")
    public void cancelarSinUsuario(String nombre) {
        api.delete(RUTA_RESERVAS + "/" + datos.idDeReserva(nombre) + "/cancelar");
    }

    @Entonces("la reserva de {string} queda en estado {string}")
    public void laReservaQuedaEnEstado(String nombre, String estado) {
        assertThat(datos.estadoDeReserva(nombre)).isEqualTo(estado);
    }

    private void reservar(Integer usuarioId, Integer horarioId, LocalDate fecha, String hora) {
        api.postJson(RUTA_RESERVAS, cuerpoReserva(usuarioId, horarioId, fecha, hora));
    }

    private static Map<String, Object> cuerpoReserva(
        Integer usuarioId,
        Integer horarioId,
        LocalDate fecha,
        String hora
    ) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("usuarioId", usuarioId);
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

    private void cancelar(Integer reservaId, Integer usuarioId) {
        api.delete(RUTA_RESERVAS + "/" + reservaId + "/cancelar?usuarioId=" + usuarioId);
    }
}
