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

    @Y("existe un segundo recurso compatible")
    public void existeUnSegundoRecursoCompatible() {
        datos.agregarRecursoCompatible("Silla 2", true);
    }

    @Y("existe otro servicio con agenda que usa el mismo recurso")
    public void existeOtroServicioConMismoRecurso() {
        datos.crearServicioAlternoParaMismoRecurso();
    }

    @Y("existe un servicio sin recursos compatibles")
    public void existeServicioSinRecursos() {
        datos.crearServicioAlternoSinRecursos();
    }

    @Cuando("{string} reserva ese horario a las {string}")
    public void reservaEseHorario(String nombre, String hora) {
        autenticarComo(nombre);
        reservar(datos.servicioId(), datos.fechaHorario(), hora);
        guardarReservaCreada(nombre);
    }

    @Cuando("{string} reserva ese horario para ayer a las {string}")
    public void reservaParaAyer(String nombre, String hora) {
        autenticarComo(nombre);
        reservar(datos.servicioId(), datos.hoy().minusDays(1), hora);
    }

    @Cuando("{string} reserva ese horario un día de la semana distinto a las {string}")
    public void reservaOtroDia(String nombre, String hora) {
        autenticarComo(nombre);
        reservar(datos.servicioId(), datos.fechaHorario().plusDays(1), hora);
    }

    @Dado("que {string} inició sesión")
    public void inicioSesion(String nombre) {
        autenticarComo(nombre);
    }

    @Y("la cuenta de {string} fue eliminada")
    public void laCuentaFueEliminada(String nombre) {
        datos.eliminarUsuario(nombre);
    }

    @Cuando("{string} reserva el servicio inexistente {int} a las {string}")
    public void reservaServicioInexistente(String nombre, int servicioId, String hora) {
        autenticarComo(nombre);
        reservar(servicioId, datos.fechaHorario(), hora);
    }

    @Cuando("envío una solicitud de reserva sin datos")
    public void envioReservaSinDatos() {
        autenticarComo("Ana");
        api.postJson(RUTA_RESERVAS, Map.of());
    }

    @Cuando("intento reservar ese horario a las {string} sin haber iniciado sesión")
    public void reservarSinSesion(String hora) {
        api.sinSesion();
        reservar(datos.servicioId(), datos.fechaHorario(), hora);
    }

    @Cuando("{string} y {string} reservan ese horario al mismo tiempo a las {string}")
    public void reservanAlMismoTiempo(String primero, String segundo, String hora) {
        autenticarComo(primero);
        autenticarComo(segundo);
        Map<String, Object> cuerpos = new LinkedHashMap<>();
        cuerpos.put(primero, cuerpoReserva(datos.servicioId(), datos.fechaHorario(), hora));
        cuerpos.put(segundo, cuerpoReserva(datos.servicioId(), datos.fechaHorario(), hora));
        api.postJsonSimultaneos(RUTA_RESERVAS, cuerpos)
            .forEach(respuesta -> codigosSimultaneos.add(respuesta.statusCode()));
    }

    @Entonces("una solicitud responde {int} y la otra responde {int}")
    public void unaRespondeYOtraResponde(int codigoA, int codigoB) {
        assertThat(codigosSimultaneos).containsExactlyInAnyOrder(codigoA, codigoB);
    }

    @Cuando("{string} reserva ese servicio a las {string}")
    public void reservaEseServicio(String nombre, String hora) {
        autenticarComo(nombre);
        reservar(datos.servicioId(), datos.fechaHorario(), hora);
        guardarReservaCreada(nombre);
    }

    @Cuando("{string} reserva el otro servicio a las {string}")
    public void reservaOtroServicio(String nombre, String hora) {
        autenticarComo(nombre);
        reservar(datos.servicioAlternoId(), datos.fechaHorario(), hora);
    }

    @Cuando("{string} reserva el servicio sin recursos a las {string}")
    public void reservaServicioSinRecursos(String nombre, String hora) {
        autenticarComo(nombre);
        reservar(datos.servicioAlternoId(), datos.fechaHorario(), hora);
    }

    @Entonces("ambas reservas usan recursos diferentes")
    public void ambasReservasUsanRecursosDiferentes() {
        assertThat(datos.recursoDeReserva("Ana"))
            .isNotEqualTo(datos.recursoDeReserva("Luis"));
    }

    @Entonces("las reservas activas usan {int} recursos diferentes")
    public void reservasActivasUsanRecursosDiferentes(int total) {
        assertThat(datos.contarRecursosDistintosDeReservasActivas()).isEqualTo(total);
    }

    @Y("la reserva queda activa para ese horario y fecha")
    public void laReservaQuedaActiva() {
        JsonNode json = api.ultimoJson();
        assertThat(json.get("estado").asString()).isEqualTo("activa");
        assertThat(json.get("horarioId").asInt()).isEqualTo(datos.horarioId());
        assertThat(json.get("servicioId").asInt()).isEqualTo(datos.servicioId());
        assertThat(json.get("recursoId").asInt()).isPositive();
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

    private void reservar(Integer servicioId, LocalDate fecha, String hora) {
        api.postJson(RUTA_RESERVAS, cuerpoReserva(servicioId, fecha, hora));
    }

    private static Map<String, Object> cuerpoReserva(Integer servicioId, LocalDate fecha, String hora) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("servicioId", servicioId);
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
