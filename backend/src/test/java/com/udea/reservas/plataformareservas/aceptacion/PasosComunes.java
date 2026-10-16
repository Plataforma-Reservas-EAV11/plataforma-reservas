package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;
import java.net.http.HttpResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@RequiredArgsConstructor
public class PasosComunes {

    private final ClienteApi api;
    private final DatosPrueba datos;

    @Before
    public void limpiarDatos() {
        datos.limpiarBaseDeDatos();
    }

    @Cuando("consulto el estado de salud del servicio")
    public void consultoElEstadoDeSalud() {
        api.get("/actuator/health");
    }

    @Cuando("consulto la ruta inexistente {string}")
    public void consultoRutaInexistente(String ruta) {
        api.get(ruta);
    }

    @Cuando("envío a {string} un cuerpo JSON malformado")
    public void envioJsonMalformado(String ruta) {
        api.postJson(ruta, "{ \"campo\": ");
    }

    @Entonces("la respuesta tiene el código HTTP {int}")
    public void laRespuestaTieneElCodigo(int codigo) {
        HttpResponse<String> respuesta = api.ultimaRespuesta();
        assertThat(respuesta.statusCode())
            .as("Código HTTP. Cuerpo recibido: %s", respuesta.body())
            .isEqualTo(codigo);
    }

    @Y("el mensaje de la respuesta es {string}")
    public void elMensajeEs(String mensaje) {
        String cuerpo = api.ultimaRespuesta().body();
        if (cuerpo.startsWith("{")) {
            JsonNode json = api.ultimoJson();
            JsonNode campo = json.get(json.has("message") ? "message" : "mensaje");
            assertThat(campo).as("Campo de mensaje en %s", cuerpo).isNotNull();
            assertThat(campo.asString()).isEqualTo(mensaje);
        } else {
            assertThat(cuerpo).isEqualTo(mensaje);
        }
    }

    @Y("el código de error es {string}")
    public void elCodigoDeErrorEs(String codigo) {
        assertThat(api.ultimoJson().get("errorCode").asString()).isEqualTo(codigo);
    }

    @Y("la respuesta indica un error en el campo {string}")
    public void laRespuestaIndicaErrorEnCampo(String campo) {
        JsonNode json = api.ultimoJson();
        boolean enErrores = json.has("errors") && json.get("errors").has(campo);
        boolean enDetalles = json.has("details")
            && json.get("details").toString().contains(campo);
        assertThat(enErrores || enDetalles)
            .as("Error del campo %s en %s", campo, json)
            .isTrue();
    }

    @Y("la respuesta no contiene el texto {string}")
    public void laRespuestaNoContiene(String texto) {
        assertThat(api.ultimaRespuesta().body()).doesNotContainIgnoringCase(texto);
    }

    @Y("la respuesta incluye un identificador de traza")
    public void laRespuestaIncluyeTraza() {
        assertThat(api.ultimoJson().get("traceId").asString()).isNotBlank();
    }

    @Y("el estado del servicio es {string}")
    public void elEstadoDelServicioEs(String estado) {
        assertThat(api.ultimoJson().get("status").asString()).isEqualTo(estado);
    }
}
