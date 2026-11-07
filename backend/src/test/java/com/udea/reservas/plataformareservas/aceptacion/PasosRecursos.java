package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@RequiredArgsConstructor
public class PasosRecursos {

    private static final String RUTA_RECURSOS = "/api/v1/providers/me/resources";

    private final ClienteApi api;
    private final DatosPrueba datos;

    private Integer recursoId;
    private Integer servicioAjenoId;

    @Dado("que el proveedor tiene un recurso de prueba")
    public void proveedorTieneUnRecursoDePrueba() {
        datos.crearHorarioHabilitado(java.time.LocalTime.of(8, 0), java.time.LocalTime.of(12, 0));
        recursoId = datos.recursoId();
    }

    @Dado("que existe un recurso de otro proveedor")
    public void existeRecursoDeOtroProveedor() {
        Integer otroUsuario = datos.crearProveedor("Otro proveedor", "otro-proveedor@test.com");
        recursoId = datos.crearRecursoDeProveedor(otroUsuario, "Recurso ajeno");
    }

    @Dado("que existe un servicio de otro proveedor")
    public void existeServicioDeOtroProveedor() {
        servicioAjenoId = datos.crearServicioDeOtroProveedor(
            "Proveedor externo",
            "proveedor-externo@test.com"
        );
    }

    @Cuando("el proveedor registra el recurso {string} de tipo {string}")
    public void proveedorRegistraRecurso(String nombre, String tipo) {
        autenticarProveedor("Proveedor");
        api.postJson(
            RUTA_RECURSOS,
            Map.of(
                "nombre", nombre,
                "tipo", tipo,
                "proveedorId", 99999
            )
        );
        if (api.ultimaRespuesta().statusCode() == 201) {
            recursoId = api.ultimoJson().get("data").get("id").asInt();
        }
    }

    @Cuando("el proveedor consulta sus recursos")
    public void proveedorConsultaSusRecursos() {
        autenticarProveedor("Proveedor");
        api.get(RUTA_RECURSOS);
    }

    @Cuando("el proveedor consulta el recurso registrado")
    @Cuando("otro proveedor consulta ese recurso")
    public void proveedorConsultaRecursoRegistrado() {
        consultResourceAsProvider();
    }

    private void consultResourceAsProvider() {
        autenticarProveedor("Proveedor");
        api.get(RUTA_RECURSOS + "/" + recursoId);
    }

    @Cuando("otro proveedor intenta actualizar ese recurso")
    public void otroProveedorActualizaRecurso() {
        autenticarProveedor("Proveedor");
        api.putJson(
            RUTA_RECURSOS + "/" + recursoId,
            Map.of("nombre", "Modificado", "tipo", "sala")
        );
    }

    @Cuando("el proveedor actualiza el recurso a {string}")
    public void proveedorActualizaRecurso(String nombre) {
        autenticarProveedor("Proveedor");
        api.putJson(
            RUTA_RECURSOS + "/" + recursoId,
            Map.of("nombre", nombre, "tipo", "consultorio")
        );
    }

    @Cuando("el proveedor desactiva el recurso")
    public void proveedorDesactivaRecurso() {
        autenticarProveedor("Proveedor");
        api.patchJson(RUTA_RECURSOS + "/" + recursoId + "/estado", Map.of("activo", false));
    }

    @Cuando("el proveedor activa el recurso")
    public void proveedorActivaRecurso() {
        autenticarProveedor("Proveedor");
        api.patchJson(RUTA_RECURSOS + "/" + recursoId + "/estado", Map.of("activo", true));
    }

    @Cuando("el proveedor asocia el recurso al servicio de prueba")
    public void proveedorAsociaRecursoServicio() {
        autenticarProveedor("Proveedor");
        api.putJson(
            RUTA_RECURSOS + "/" + recursoId + "/servicios/" + datos.servicioId(),
            Map.of()
        );
    }

    @Cuando("el proveedor retira el recurso del servicio de prueba")
    public void proveedorRetiraRecursoServicio() {
        autenticarProveedor("Proveedor");
        api.delete(RUTA_RECURSOS + "/" + recursoId + "/servicios/" + datos.servicioId());
    }

    @Cuando("el proveedor intenta asociar el recurso al servicio ajeno")
    public void proveedorAsociaServicioAjeno() {
        autenticarProveedor("Proveedor");
        api.putJson(
            RUTA_RECURSOS + "/" + recursoId + "/servicios/" + servicioAjenoId,
            Map.of()
        );
    }

    @Cuando("el proveedor desactiva el recurso con una reserva histórica")
    public void proveedorDesactivaRecursoConReserva() {
        recursoId = datos.recursoId();
        autenticarProveedor("Proveedor");
        api.patchJson(RUTA_RECURSOS + "/" + recursoId + "/estado", Map.of("activo", false));
    }

    @Entonces("el recurso queda registrado y activo")
    public void recursoQuedaRegistradoYActivo() {
        JsonNode response = api.ultimoJson().get("data");
        assertThat(response.get("activo").asBoolean()).isTrue();
        assertThat(response.get("nombre").asString()).isNotBlank();
    }

    @Entonces("el proveedor ve únicamente sus recursos")
    public void proveedorVeUnicamenteSusRecursos() {
        JsonNode resources = api.ultimoJson().get("data");
        assertThat(resources.size()).isEqualTo(2);
        assertThat(ids(resources)).contains(recursoId);
    }

    @Entonces("el recurso consultado pertenece al proveedor autenticado")
    public void recursoConsultadoEsPropio() {
        assertThat(api.ultimoJson().get("data").get("id").asInt()).isEqualTo(recursoId);
    }

    @Entonces("los datos del recurso quedan actualizados")
    public void datosRecursoActualizados() {
        assertThat(api.ultimoJson().get("data").get("nombre").asString())
            .isEqualTo("Sala renovada");
    }

    @Entonces("el servicio tiene el recurso asociado")
    public void servicioTieneRecursoAsociado() {
        assertThat(datos.contarRecursosCompatibles(datos.servicioId())).isEqualTo(2);
    }

    @Entonces("el recurso deja de estar asociado al servicio")
    public void recursoDesasociadoDelServicio() {
        assertThat(datos.contarRecursosCompatibles(datos.servicioId())).isEqualTo(1);
    }

    @Entonces("la reserva histórica sigue activa")
    public void reservaHistoricaSigueActiva() {
        assertThat(datos.estadoDeReserva("Ana")).isEqualTo("activa");
        assertThat(datos.contarReservasActivasDeUsuario("Ana")).isEqualTo(1);
    }

    @Entonces("el recurso queda desactivado sin borrar sus datos")
    public void recursoDesactivadoSinBorrado() {
        assertThat(api.ultimoJson().get("data").get("activo").asBoolean()).isFalse();
        assertThat(datos.recursoDeReserva("Ana")).isEqualTo(recursoId);
    }

    private void autenticarProveedor(String nombre) {
        if (recursoId == null && "Proveedor".equals(nombre)) {
            recursoId = datos.recursoId();
        }
        if (!api.haySesion(nombre)) {
            api.iniciarSesion(
                nombre,
                datos.emailDeUsuario(nombre),
                DatosPrueba.CONTRASENA_POR_DEFECTO
            );
        }
        api.usarSesion(nombre);
    }

    private static List<Integer> ids(JsonNode resources) {
        java.util.ArrayList<Integer> ids = new java.util.ArrayList<>();
        for (JsonNode resource : resources) {
            ids.add(resource.get("id").asInt());
        }
        return ids;
    }
}
