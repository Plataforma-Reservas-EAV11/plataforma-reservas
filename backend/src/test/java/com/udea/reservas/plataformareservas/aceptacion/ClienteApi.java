package com.udea.reservas.plataformareservas.aceptacion;

import io.cucumber.spring.ScenarioScope;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Cliente HTTP de caja negra: solo conoce la API publicada, igual que Postman.
 * Cada escenario tiene su propia instancia. Puede mantener varias sesiones a la
 * vez (una por persona, cada una con sus cookies y su token CSRF) y siempre usa
 * la "sesión actual"; una sesión nueva sin iniciar sesión representa a quien
 * llama sin haberse autenticado.
 */
@Component
@ScenarioScope
public class ClienteApi {

    private static final String RUTA_CSRF = "/api/v1/auth/csrf";
    private static final String RUTA_LOGIN = "/api/v1/auth/login";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Sesion> sesiones = new HashMap<>();

    @Value("${local.server.port}")
    private int puerto;

    private Sesion sesionActual = new Sesion();
    private HttpResponse<String> ultimaRespuesta;

    /** Cookies y token CSRF de una persona que usa la API. */
    private static final class Sesion {
        private final HttpClient httpClient = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
        private String cabeceraCsrf;
        private String tokenCsrf;
    }

    public boolean haySesion(String clave) {
        return sesiones.containsKey(clave);
    }

    public void usarSesion(String clave) {
        sesionActual = sesiones.get(clave);
    }

    /** Pasa a una persona sin sesión iniciada (sin cookies ni token). */
    public void sinSesion() {
        sesionActual = new Sesion();
    }

    /**
     * Pide el token CSRF e inicia sesión con una sesión nueva, que queda como la
     * actual y se guarda con la clave indicada. La respuesta del inicio de sesión
     * es la última respuesta, sea cual sea su resultado.
     */
    public HttpResponse<String> iniciarSesion(String clave, String email, String contrasena) {
        Sesion sesion = new Sesion();
        sesionActual = sesion;
        sesiones.put(clave, sesion);
        JsonNode csrf = json(get(RUTA_CSRF));
        sesion.cabeceraCsrf = csrf.get("headerName").asString();
        sesion.tokenCsrf = csrf.get("token").asString();
        return postJson(RUTA_LOGIN, Map.of("email", email, "password", contrasena));
    }

    public HttpResponse<String> get(String ruta) {
        return enviar(sesionActual, peticion(ruta).GET().build());
    }

    public HttpResponse<String> delete(String ruta) {
        HttpRequest.Builder builder = peticion(ruta).DELETE();
        agregarCsrf(sesionActual, builder);
        return enviar(sesionActual, builder.build());
    }

    public HttpResponse<String> postJson(String ruta, Object cuerpo) {
        return enviar(sesionActual, construirPost(sesionActual, ruta, cuerpo));
    }

    /**
     * Envía varias solicitudes POST que salen exactamente al mismo tiempo, cada una
     * con la sesión indicada, para reproducir la condición de carrera de dos
     * usuarios reservando a la vez. Las claves son las de las sesiones.
     */
    public List<HttpResponse<String>> postJsonSimultaneos(String ruta, Map<String, Object> cuerposPorSesion) {
        CyclicBarrier salida = new CyclicBarrier(cuerposPorSesion.size());
        try (ExecutorService hilos = Executors.newFixedThreadPool(cuerposPorSesion.size())) {
            List<CompletableFuture<HttpResponse<String>>> envios = cuerposPorSesion.entrySet().stream()
                .map(entrada -> CompletableFuture.supplyAsync(() -> {
                    Sesion sesion = sesiones.get(entrada.getKey());
                    HttpRequest peticion = construirPost(sesion, ruta, entrada.getValue());
                    esperar(salida);
                    return enviar(sesion, peticion);
                }, hilos))
                .toList();
            return envios.stream().map(CompletableFuture::join).toList();
        }
    }

    public HttpResponse<String> ultimaRespuesta() {
        return ultimaRespuesta;
    }

    public JsonNode json(HttpResponse<String> respuesta) {
        return objectMapper.readTree(respuesta.body());
    }

    public JsonNode ultimoJson() {
        return json(ultimaRespuesta);
    }

    private HttpRequest construirPost(Sesion sesion, String ruta, Object cuerpo) {
        String json = cuerpo instanceof String texto ? texto : objectMapper.writeValueAsString(cuerpo);
        HttpRequest.Builder builder = peticion(ruta)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json));
        agregarCsrf(sesion, builder);
        return builder.build();
    }

    private static void agregarCsrf(Sesion sesion, HttpRequest.Builder builder) {
        if (sesion.tokenCsrf != null) {
            builder.header(sesion.cabeceraCsrf, sesion.tokenCsrf);
        }
    }

    private HttpRequest.Builder peticion(String ruta) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta));
    }

    private static void esperar(CyclicBarrier salida) {
        try {
            salida.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (BrokenBarrierException e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpResponse<String> enviar(Sesion sesion, HttpRequest peticion) {
        try {
            HttpResponse<String> respuesta = sesion.httpClient.send(peticion, HttpResponse.BodyHandlers.ofString());
            ultimaRespuesta = respuesta;
            return respuesta;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo enviar la solicitud a " + peticion.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Solicitud interrumpida: " + peticion.uri(), e);
        }
    }
}
