package com.udea.reservas.plataformareservas.aceptacion;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Y;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@RequiredArgsConstructor
public class PasosAutenticacion {

    private static final String RUTA_REGISTRO = "/api/v1/auth/register";
    private static final String RUTA_REGISTRO_PROVEEDOR = "/api/v1/providers/register";
    private static final String RUTA_LOGIN = "/api/v1/auth/login";

    private final ClienteApi api;
    private final DatosPrueba datos;

    @Dado("que no existe una cuenta con el correo {string}")
    public void noExisteCuenta(String email) {
        assertThat(datos.contarUsuarios()).isZero();
    }

    @Dado("que existe una cuenta con el correo {string}")
    public void existeCuenta(String email) {
        datos.crearUsuario("Cuenta existente", email, DatosPrueba.CONTRASENA_POR_DEFECTO, "usuario");
    }

    @Dado("que existe una cuenta con el correo {string} y la contraseña {string}")
    public void existeCuentaConContrasena(String email, String contrasena) {
        datos.crearUsuario("Ana", email, contrasena, "usuario");
    }

    @Cuando("me registro con nombre {string}, correo {string}, contraseña {string} y confirmación {string}")
    public void meRegistro(String nombre, String email, String contrasena, String confirmacion) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("name", nombre);
        cuerpo.put("email", email);
        cuerpo.put("password", contrasena);
        cuerpo.put("confirmPassword", confirmacion);
        api.postJson(RUTA_REGISTRO, cuerpo);
    }

    @Cuando("me registro con una contraseña formada por {int} veces el carácter {string}")
    public void meRegistroConContrasenaLarga(int repeticiones, String caracter) {
        String contrasena = caracter.repeat(repeticiones);
        meRegistro("Ana Pérez", "ana@example.com", contrasena, contrasena);
    }

    @Y("la cuenta {string} queda guardada con la contraseña cifrada y el rol {string}")
    public void cuentaGuardadaConContrasenaCifrada(String email, String rol) {
        Map<String, Object> usuario = datos.usuarioPorEmail(email);
        String hash = (String) usuario.get("password_hash");
        assertThat(hash).startsWith("$2").doesNotContain(DatosPrueba.CONTRASENA_POR_DEFECTO);
        assertThat(usuario).containsEntry("rol", rol);
    }

    @Y("existen {int} cuentas registradas")
    public void existenCuentas(int total) {
        assertThat(datos.contarUsuarios()).isEqualTo(total);
    }

    @Cuando("me registro como proveedor con nombre {string}, negocio {string}, correo {string}, contraseña {string} y confirmación {string}")
    public void meRegistroComoProveedor(
        String nombre,
        String negocio,
        String email,
        String contrasena,
        String confirmacion
    ) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("name", nombre);
        cuerpo.put("businessName", negocio);
        cuerpo.put("email", email);
        cuerpo.put("password", contrasena);
        cuerpo.put("confirmPassword", confirmacion);
        cuerpo.put("phone", "3001234567");
        api.postJson(RUTA_REGISTRO_PROVEEDOR, cuerpo);
    }

    @Y("existe un proveedor con el negocio {string}")
    public void existeProveedor(String negocio) {
        assertThat(datos.contarProveedoresConNegocio(negocio)).isEqualTo(1);
    }

    @Cuando("inicio sesión con el correo {string} y la contraseña {string}")
    public void inicioSesion(String email, String contrasena) {
        api.iniciarSesion("actual", email, contrasena);
    }

    @Cuando("inicio sesión sin token CSRF con el correo {string} y la contraseña {string}")
    public void inicioSesionSinCsrf(String email, String contrasena) {
        api.sinSesion();
        api.postJson(RUTA_LOGIN, Map.of("email", email, "password", contrasena));
    }

    @Dado("que inicié sesión con una cuenta de usuario")
    public void inicieSesionConUnaCuentaDeUsuario() {
        datos.crearUsuario("Sesion");
        api.iniciarSesion("Sesion", datos.emailDeUsuario("Sesion"), DatosPrueba.CONTRASENA_POR_DEFECTO);
    }

    @Cuando("intento iniciar sesión {int} veces con el correo {string} y una contraseña incorrecta")
    public void intentoIniciarSesionVariasVeces(int intentos, String email) {
        for (int i = 0; i < intentos; i++) {
            api.iniciarSesion("fallido-" + i, email, "contrasena-incorrecta");
        }
    }

    @Y("el bloqueo de la cuenta {string} ya venció")
    public void elBloqueoYaVencio(String email) {
        datos.vencerBloqueo(email);
    }

    @Y("la respuesta contiene los datos públicos del usuario {string}")
    public void respuestaContieneDatosPublicos(String email) {
        JsonNode data = api.ultimoJson().get("data");
        assertThat(data.get("email").asString()).isEqualTo(email);
        assertThat(data.has("id")).isTrue();
        assertThat(data.has("password")).isFalse();
        assertThat(data.has("passwordHash")).isFalse();
    }
}
