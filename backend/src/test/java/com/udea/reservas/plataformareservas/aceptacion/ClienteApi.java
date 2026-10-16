package com.udea.reservas.plataformareservas.aceptacion;

import io.cucumber.spring.ScenarioScope;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * Cada escenario tiene su propia instancia, con su propia sesión y cookies.
 */
@Component
@ScenarioScope
public class ClienteApi {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .build();

    @Value("${local.server.port}")
    private int puerto;

    private HttpResponse<String> ultimaRespuesta;

    public HttpResponse<String> get(String ruta) {
        return enviar(peticion(ruta).GET().build());
    }

    public HttpResponse<String> delete(String ruta) {
        return enviar(peticion(ruta).DELETE().build());
    }

    public HttpResponse<String> postJson(String ruta, Object cuerpo) {
        return postJson(ruta, cuerpo, Map.of());
    }

    public HttpResponse<String> postJson(String ruta, Object cuerpo, Map<String, String> cabeceras) {
        String json = cuerpo instanceof String texto ? texto : objectMapper.writeValueAsString(cuerpo);
        HttpRequest.Builder builder = peticion(ruta)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json));
        cabeceras.forEach(builder::header);
        return enviar(builder.build());
    }

    /**
     * Envía varias solicitudes POST que salen exactamente al mismo tiempo, para
     * reproducir la condición de carrera de dos usuarios reservando a la vez.
     */
    public List<HttpResponse<String>> postJsonSimultaneos(String ruta, List<?> cuerpos) {
        CyclicBarrier salida = new CyclicBarrier(cuerpos.size());
        try (ExecutorService hilos = Executors.newFixedThreadPool(cuerpos.size())) {
            List<CompletableFuture<HttpResponse<String>>> envios = cuerpos.stream()
                .map(cuerpo -> CompletableFuture.supplyAsync(() -> {
                    esperar(salida);
                    return enviar(peticion(ruta)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(cuerpo)))
                        .build());
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

    private HttpResponse<String> enviar(HttpRequest peticion) {
        try {
            HttpResponse<String> respuesta = httpClient.send(peticion, HttpResponse.BodyHandlers.ofString());
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
