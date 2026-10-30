package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasosAgenda {

    private final ClienteApi api;
    private final DatosPrueba datos;

    private Integer otraAgendaId;

    @Dado("que soy un proveedor con una agenda activa")
    public void soyProveedorConAgendaActiva() {
        datos.crearProveedorConAgendaActiva();
        api.iniciarSesion("Proveedor", "proveedor@test.com", DatosPrueba.CONTRASENA_POR_DEFECTO);
    }

    @Y("existe otro proveedor con su propia agenda")
    public void existeOtroProveedor() {
        otraAgendaId = datos.crearProveedorConAgenda("Otro proveedor", "otro@test.com");
    }

    @Cuando("intento definir un bloque en la agenda del otro proveedor")
    public void definoBloqueEnAgendaAjena() {
        api.postJson(rutaHorarios(otraAgendaId), cuerpoBloque());
    }

    @Cuando("intento definir un bloque en mi agenda sin haber iniciado sesión")
    public void definoBloqueSinSesion() {
        api.sinSesion();
        api.postJson(rutaHorarios(datos.agendaId()), cuerpoBloque());
    }

    @Y("la agenda del otro proveedor tiene {int} bloque(s) guardado(s)")
    public void laAgendaAjenaTieneBloques(int total) {
        assertThat(datos.contarBloquesDeLaAgenda(otraAgendaId)).isEqualTo(total);
    }

    @Y("mi agenda tiene un bloque el día {int} de {string} a {string}")
    public void miAgendaTieneUnBloque(int dia, String inicio, String fin) {
        datos.crearBloque(dia, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    @Cuando("defino un bloque el día {int} de {string} a {string} con turnos de {int} minutos")
    public void definoUnBloque(int dia, String inicio, String fin, int duracion) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("diaSemana", dia);
        cuerpo.put("horaInicio", inicio);
        cuerpo.put("horaFin", fin);
        cuerpo.put("duracionSlotMin", duracion);
        api.postJson(rutaHorarios(datos.agendaId()), cuerpo);
    }

    @Cuando("defino un bloque el día {int} desde las {string} sin hora de fin")
    public void definoUnBloqueSinHoraFin(int dia, String inicio) {
        api.postJson(
            rutaHorarios(datos.agendaId()),
            Map.of("diaSemana", dia, "horaInicio", inicio, "duracionSlotMin", 30)
        );
    }

    @Cuando("defino un bloque válido en la agenda {int}")
    public void definoUnBloqueEnLaAgenda(int agendaId) {
        api.postJson(
            rutaHorarios(agendaId),
            Map.of("diaSemana", 1, "horaInicio", "08:00:00", "horaFin", "12:00:00", "duracionSlotMin", 30)
        );
    }

    @Y("mi agenda tiene {int} bloque(s) guardado(s)")
    public void miAgendaTieneBloques(int total) {
        assertThat(datos.contarBloquesDeLaAgenda()).isEqualTo(total);
    }

    @Y("la respuesta muestra el bloque del día {int} de {string} a {string}")
    public void laRespuestaMuestraElBloque(int dia, String inicio, String fin) {
        var data = api.ultimoJson().get("data");
        assertThat(data.get("diaSemana").asInt()).isEqualTo(dia);
        assertThat(LocalTime.parse(data.get("horaInicio").asString())).isEqualTo(LocalTime.parse(inicio));
        assertThat(LocalTime.parse(data.get("horaFin").asString())).isEqualTo(LocalTime.parse(fin));
        assertThat(data.get("agendaId").asInt()).isEqualTo(datos.agendaId());
    }

    private static Map<String, Object> cuerpoBloque() {
        return Map.of("diaSemana", 1, "horaInicio", "08:00:00", "horaFin", "12:00:00", "duracionSlotMin", 30);
    }

    private static String rutaHorarios(Integer agendaId) {
        return "/api/agendas/" + agendaId + "/horarios";
    }
}
