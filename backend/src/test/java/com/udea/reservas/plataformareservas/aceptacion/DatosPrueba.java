package com.udea.reservas.plataformareservas.aceptacion;

import io.cucumber.spring.ScenarioScope;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Prepara y consulta los datos de cada escenario directamente en la base de datos,
 * para que cada caso dependa solo de lo que prueba y no de otros endpoints.
 */
@Component
@ScenarioScope
@RequiredArgsConstructor
public class DatosPrueba {

    public static final String CONTRASENA_POR_DEFECTO = "clave-segura-1";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private final Map<String, Integer> usuariosPorNombre = new HashMap<>();
    private final Map<String, String> emailsPorNombre = new HashMap<>();
    private final Map<String, Integer> reservasPorUsuario = new HashMap<>();

    private Integer agendaId;
    private Integer horarioId;
    private LocalDate fechaHorario;

    public void limpiarBaseDeDatos() {
        jdbc.execute(
            "TRUNCATE reserva, horario_disponible, agenda, recurso, proveedor, usuario RESTART IDENTITY CASCADE"
        );
    }

    public Integer crearUsuario(String nombre, String email, String contrasena, String rol) {
        Integer id = jdbc.queryForObject(
            "INSERT INTO usuario (nombre, email, password_hash, rol) VALUES (?, ?, ?, ?) RETURNING id",
            Integer.class,
            nombre,
            email,
            passwordEncoder.encode(contrasena),
            rol
        );
        usuariosPorNombre.put(nombre, id);
        emailsPorNombre.put(nombre, email);
        return id;
    }

    public Integer crearUsuario(String nombre) {
        String email = nombre.toLowerCase(Locale.ROOT) + "@test.com";
        return crearUsuario(nombre, email, CONTRASENA_POR_DEFECTO, "usuario");
    }

    public Integer idDeUsuario(String nombre) {
        return usuariosPorNombre.get(nombre);
    }

    public String emailDeUsuario(String nombre) {
        return emailsPorNombre.get(nombre);
    }

    public void eliminarUsuario(String nombre) {
        jdbc.update("DELETE FROM usuario WHERE id = ?", idDeUsuario(nombre));
    }

    /** Simula que ya pasó el tiempo de bloqueo (un día cubre cualquier diferencia de zona horaria con la base). */
    public void vencerBloqueo(String email) {
        jdbc.update("UPDATE usuario SET bloqueado_hasta = now() - interval '1 day' WHERE email = ?", email);
    }

    public void crearProveedorConAgendaActiva() {
        agendaId = crearProveedorConAgenda("Proveedor", "proveedor@test.com");
    }

    /** Crea un proveedor (cuenta, negocio, recurso y agenda activa) y devuelve el id de su agenda. */
    public Integer crearProveedorConAgenda(String nombre, String email) {
        Integer usuarioId = crearUsuario(nombre, email, CONTRASENA_POR_DEFECTO, "proveedor");
        Integer proveedorId = jdbc.queryForObject(
            "INSERT INTO proveedor (usuario_id, nombre_negocio) VALUES (?, 'Barbería Style') RETURNING id",
            Integer.class,
            usuarioId
        );
        Integer recursoId = jdbc.queryForObject(
            "INSERT INTO recurso (proveedor_id, nombre, tipo) VALUES (?, 'Silla 1', 'silla') RETURNING id",
            Integer.class,
            proveedorId
        );
        return jdbc.queryForObject(
            "INSERT INTO agenda (proveedor_id, recurso_id, nombre, activa) VALUES (?, ?, 'Agenda principal', true) RETURNING id",
            Integer.class,
            proveedorId,
            recursoId
        );
    }

    public Integer agendaId() {
        return agendaId;
    }

    public void desactivarAgenda() {
        jdbc.update("UPDATE agenda SET activa = false WHERE id = ?", agendaId);
    }

    public void crearBloque(int diaSemana, LocalTime inicio, LocalTime fin) {
        horarioId = jdbc.queryForObject(
            "INSERT INTO horario_disponible (agenda_id, dia_semana, hora_inicio, hora_fin, duracion_slot_min) VALUES (?, ?, ?, ?, 30) RETURNING id",
            Integer.class,
            agendaId,
            diaSemana,
            inicio,
            fin
        );
    }

    /**
     * Crea un bloque en el día de la semana de una fecha que cae dentro de una semana,
     * para que la fecha de la reserva siempre esté en el futuro.
     */
    public void crearHorarioHabilitado(LocalTime inicio, LocalTime fin) {
        crearProveedorConAgendaActiva();
        fechaHorario = LocalDate.now(clock).plusDays(7);
        crearBloque(fechaHorario.getDayOfWeek().getValue() - 1, inicio, fin);
    }

    public LocalDate hoy() {
        return LocalDate.now(clock);
    }

    public Integer horarioId() {
        return horarioId;
    }

    public LocalDate fechaHorario() {
        return fechaHorario;
    }

    public Integer crearReserva(String nombreUsuario, LocalTime hora, String estado) {
        Integer id = jdbc.queryForObject(
            "INSERT INTO reserva (usuario_id, horario_id, fecha, hora, estado) VALUES (?, ?, ?, ?, ?) RETURNING id",
            Integer.class,
            idDeUsuario(nombreUsuario),
            horarioId,
            fechaHorario,
            hora,
            estado
        );
        reservasPorUsuario.put(nombreUsuario, id);
        return id;
    }

    public void registrarReserva(String nombreUsuario, Integer reservaId) {
        reservasPorUsuario.put(nombreUsuario, reservaId);
    }

    public Integer idDeReserva(String nombreUsuario) {
        return reservasPorUsuario.get(nombreUsuario);
    }

    public void cambiarEstadoReserva(String nombreUsuario, String estado) {
        jdbc.update("UPDATE reserva SET estado = ? WHERE id = ?", estado, idDeReserva(nombreUsuario));
    }

    public String estadoDeReserva(String nombreUsuario) {
        return jdbc.queryForObject(
            "SELECT estado FROM reserva WHERE id = ?",
            String.class,
            idDeReserva(nombreUsuario)
        );
    }

    public int contarReservasActivasDelHorario() {
        return contar(
            "SELECT count(*) FROM reserva WHERE horario_id = ? AND fecha = ? AND estado = 'activa'",
            horarioId,
            fechaHorario
        );
    }

    public int contarBloquesDeLaAgenda() {
        return contar("SELECT count(*) FROM horario_disponible WHERE agenda_id = ?", agendaId);
    }

    public int contarBloquesDeLaAgenda(Integer idAgenda) {
        return contar("SELECT count(*) FROM horario_disponible WHERE agenda_id = ?", idAgenda);
    }

    public int contarUsuarios() {
        return contar("SELECT count(*) FROM usuario");
    }

    public Map<String, Object> usuarioPorEmail(String email) {
        return jdbc.queryForMap("SELECT nombre, email, password_hash, rol FROM usuario WHERE email = ?", email);
    }

    public int contarProveedoresConNegocio(String negocio) {
        return contar("SELECT count(*) FROM proveedor WHERE nombre_negocio = ?", negocio);
    }

    private int contar(String sql, Object... argumentos) {
        Integer total = jdbc.queryForObject(sql, Integer.class, argumentos);
        return total == null ? 0 : total;
    }
}
