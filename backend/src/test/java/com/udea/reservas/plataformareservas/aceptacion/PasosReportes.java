package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.time.LocalDate;
import java.time.LocalTime;
import tools.jackson.databind.JsonNode;

public class PasosReportes {

    private static final String RUTA_REPORTE = "/api/reportes/ocupacion";

    private final ClienteApi api;
    private final DatosPrueba datos;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public PasosReportes(ClienteApi api, DatosPrueba datos) {
        this.api = api;
        this.datos = datos;
    }

    @Dado("que existen reservas registradas en la plataforma")
    public void existenReservasRegistradas() {
        prepararAgendaYUsuario();
        datos.crearReserva("Ana", LocalTime.of(9, 0), "activa");
        fechaInicio = datos.hoy().plusDays(1);
        fechaFin = datos.hoy().plusDays(14);
    }

    @Dado("que no hay reservas registradas en el periodo consultado")
    public void noHayReservasEnPeriodo() {
        prepararAgendaYUsuario();
        datos.crearReserva("Ana", LocalTime.of(9, 0), "activa");
        fechaInicio = datos.hoy().plusDays(1);
        fechaFin = datos.hoy().plusDays(2);
    }

    @Dado("que estoy configurando el periodo del reporte")
    public void estoyConfigurandoPeriodo() {
        fechaInicio = datos.hoy().plusDays(10);
        fechaFin = datos.hoy().plusDays(1);
    }

    @Dado("que existen reservas dentro y fuera del periodo consultado")
    public void existenReservasDentroYFuera() {
        prepararAgendaYUsuario();
        datos.crearReserva("Ana", LocalTime.of(9, 0), "activa");
        datos.crearReservaEnFecha(
            "Ana",
            datos.hoy().plusDays(20),
            LocalTime.of(9, 0),
            "completada"
        );
        fechaInicio = datos.hoy().plusDays(1);
        fechaFin = datos.hoy().plusDays(14);
    }

    @Cuando("consulto el reporte de un periodo determinado")
    public void consultoReportePeriodoDeterminado() {
        consultarReporte();
    }

    @Cuando("consulto el reporte de ese periodo")
    public void consultoReporteDeEsePeriodo() {
        consultarReporte();
    }

    @Cuando("ingreso una fecha de inicio posterior a la fecha de fin")
    public void ingresoRangoInvalido() {
        consultarReporte();
    }

    @Cuando("consulto el reporte de un periodo específico")
    public void consultoReportePeriodoEspecifico() {
        consultarReporte();
    }

    @Entonces("el reporte muestra {int} reserva realizada")
    @Entonces("el reporte muestra {int} reservas realizadas")
    public void reporteMuestraTotal(int cantidad) {
        assertThat(api.ultimoJson().get("totalReservas").asLong()).isEqualTo(cantidad);
    }

    @Y("el reporte muestra la ocupación del servicio {string} con {int} reserva")
    @Y("el reporte solo incluye el servicio {string} con {int} reserva")
    public void reporteMuestraOcupacionServicio(String nombre, int cantidad) {
        JsonNode servicio = buscarPorNombre(
            api.ultimoJson().get("ocupacionPorServicio"),
            "nombreServicio",
            nombre
        );
        assertThat(servicio).isNotNull();
        assertThat(servicio.get("cantidadReservas").asInt()).isEqualTo(cantidad);
    }

    @Y("el reporte muestra el uso del recurso {string} con {int} reserva")
    @Y("el reporte solo incluye el recurso {string} con {int} reserva")
    public void reporteMuestraUsoRecurso(String nombre, int cantidad) {
        JsonNode recurso = buscarPorNombre(
            api.ultimoJson().get("usoPorRecurso"),
            "nombreRecurso",
            nombre
        );
        assertThat(recurso).isNotNull();
        assertThat(recurso.get("cantidadUso").asInt()).isEqualTo(cantidad);
    }

    @Y("el reporte muestra los valores de ocupación y uso en cero")
    public void reporteMuestraValoresEnCero() {
        JsonNode reporte = api.ultimoJson();
        assertThat(reporte.get("ocupacionPorServicio")).isEmpty();
        assertThat(reporte.get("usoPorRecurso")).isEmpty();
    }

    @Y("el reporte indica que no hay reservas en el periodo seleccionado")
    public void reporteIndicaPeriodoSinReservas() {
        JsonNode reporte = api.ultimoJson();
        assertThat(reporte.get("totalReservas").asLong()).isZero();
        assertThat(mensajeDe(reporte))
            .as("Mensaje del reporte sin reservas")
            .isNotNull()
            .asString()
            .containsIgnoringCase("No hay reservas en el periodo seleccionado");
    }

    private JsonNode mensajeDe(JsonNode respuesta) {
        if (respuesta.has("message")) {
            return respuesta.get("message");
        }
        return respuesta.get("mensaje");
    }

    @Y("no se genera el reporte")
    public void noSeGeneraReporte() {
        assertThat(api.ultimaRespuesta().statusCode()).isEqualTo(400);
    }

    private void prepararAgendaYUsuario() {
        datos.crearHorarioHabilitado(LocalTime.of(8, 0), LocalTime.of(12, 0));
        datos.crearUsuario("Ana");
    }

    private void consultarReporte() {
        api.get(
            RUTA_REPORTE
                + "?fechaInicio=" + fechaInicio
                + "&fechaFin=" + fechaFin
        );
    }

    private static JsonNode buscarPorNombre(JsonNode elementos, String campo, String nombre) {
        for (JsonNode elemento : elementos) {
            if (nombre.equals(elemento.get(campo).asString())) {
                return elemento;
            }
        }
        return null;
    }
}
