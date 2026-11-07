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
    private Integer proveedorId;
    private Integer servicioId;
    private Integer servicioAlternoId;
    private Integer recursoId;
    private LocalDate fechaHorario;

    public void limpiarBaseDeDatos() {
        jdbc.execute(
            "TRUNCATE reserva, horario_disponible, agenda, servicio_recurso, recurso, servicio, proveedor, usuario RESTART IDENTITY CASCADE"
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

    public Integer crearProveedor(String nombre, String email) {
        Integer usuarioId = crearUsuario(
            nombre,
            email,
            CONTRASENA_POR_DEFECTO,
            "proveedor"
        );
        crearPerfilProveedor(usuarioId, nombre + " negocio");
        return usuarioId;
    }

    public Integer crearPerfilProveedor(Integer usuarioId, String nombreNegocio) {
        return jdbc.queryForObject(
            "INSERT INTO proveedor (usuario_id, nombre_negocio) VALUES (?, ?) RETURNING id",
            Integer.class,
            usuarioId,
            nombreNegocio
        );
    }

    public int contarServiciosDeUsuario(Integer usuarioId) {
        return jdbc.queryForObject(
            "SELECT count(*) FROM servicio s JOIN proveedor p ON p.id = s.proveedor_id WHERE p.usuario_id = ?",
            Integer.class,
            usuarioId
        );
    }

    public Integer crearRecursoDeProveedor(Integer usuarioId, String nombre) {
        Integer idProveedor = jdbc.queryForObject(
            "SELECT id FROM proveedor WHERE usuario_id = ?",
            Integer.class,
            usuarioId
        );
        return jdbc.queryForObject(
            "INSERT INTO recurso (proveedor_id, nombre, tipo, activo) VALUES (?, ?, 'sala', true) RETURNING id",
            Integer.class,
            idProveedor,
            nombre
        );
    }

    public int contarRecursosCompatibles(Integer idServicio) {
        return contar(
            "SELECT count(*) FROM servicio_recurso WHERE servicio_id = ?",
            idServicio
        );
    }

    public Integer crearServicioDeOtroProveedor(String nombre, String email) {
        Integer otroUsuario = crearUsuario(
            nombre,
            email,
            CONTRASENA_POR_DEFECTO,
            "proveedor"
        );
        Integer otroProveedor = crearPerfilProveedor(otroUsuario, nombre + " negocio");
        return jdbc.queryForObject(
            "INSERT INTO servicio (proveedor_id, nombre, descripcion, duracion_minutos) VALUES (?, 'Servicio externo', 'Servicio de otro proveedor', 30) RETURNING id",
            Integer.class,
            otroProveedor
        );
    }

    public int contarReservasActivasDeUsuario(String nombreUsuario) {
        return contar(
            "SELECT count(*) FROM reserva WHERE usuario_id = ? AND estado = 'activa'",
            idDeUsuario(nombreUsuario)
        );
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

    /** Crea un proveedor, servicio, recurso compatible y agenda activa para las pruebas de reservas. */
    public Integer crearProveedorConAgenda(String nombre, String email) {
        Integer usuarioId = crearUsuario(nombre, email, CONTRASENA_POR_DEFECTO, "proveedor");
        proveedorId = jdbc.queryForObject(
            "INSERT INTO proveedor (usuario_id, nombre_negocio) VALUES (?, 'Barbería Style') RETURNING id",
            Integer.class,
            usuarioId
        );
        servicioId = jdbc.queryForObject(
            "INSERT INTO servicio (proveedor_id, nombre, descripcion, duracion_minutos) VALUES (?, 'Corte', 'Corte de cabello', 30) RETURNING id",
            Integer.class,
            proveedorId
        );
        recursoId = insertarRecurso("Silla 1", true);
        jdbc.update(
            "INSERT INTO servicio_recurso (servicio_id, recurso_id) VALUES (?, ?)",
            servicioId,
            recursoId
        );
        agendaId = jdbc.queryForObject(
            "INSERT INTO agenda (proveedor_id, servicio_id, nombre, activa) VALUES (?, ?, 'Agenda principal', true) RETURNING id",
            Integer.class,
            proveedorId,
            servicioId
        );
        return agendaId;
    }

    public void agregarRecursoCompatible(String nombre, boolean activo) {
        Integer nuevoRecursoId = insertarRecurso(nombre, activo);
        jdbc.update(
            "INSERT INTO servicio_recurso (servicio_id, recurso_id) VALUES (?, ?)",
            servicioId,
            nuevoRecursoId
        );
    }

    public void crearServicioAlternoParaMismoRecurso() {
        servicioAlternoId = jdbc.queryForObject(
            "INSERT INTO servicio (proveedor_id, nombre, descripcion, duracion_minutos) VALUES (?, 'Afeitado', 'Afeitado de barba', 30) RETURNING id",
            Integer.class,
            proveedorId
        );
        jdbc.update(
            "INSERT INTO servicio_recurso (servicio_id, recurso_id) VALUES (?, ?)",
            servicioAlternoId,
            recursoId
        );
        Integer agendaAlternaId = jdbc.queryForObject(
            "INSERT INTO agenda (proveedor_id, servicio_id, nombre, activa) VALUES (?, ?, 'Agenda alterna', true) RETURNING id",
            Integer.class,
            proveedorId,
            servicioAlternoId
        );
        jdbc.update(
            "INSERT INTO horario_disponible (agenda_id, dia_semana, hora_inicio, hora_fin, duracion_slot_min) VALUES (?, ?, '08:00', '12:00', 30)",
            agendaAlternaId,
            fechaHorario.getDayOfWeek().getValue() - 1
        );
    }

    public void crearServicioAlternoSinRecursos() {
        servicioAlternoId = jdbc.queryForObject(
            "INSERT INTO servicio (proveedor_id, nombre, descripcion, duracion_minutos) VALUES (?, 'Consulta virtual', 'Asesoría virtual', 30) RETURNING id",
            Integer.class,
            proveedorId
        );
        Integer agendaAlternaId = jdbc.queryForObject(
            "INSERT INTO agenda (proveedor_id, servicio_id, nombre, activa) VALUES (?, ?, 'Agenda sin recursos', true) RETURNING id",
            Integer.class,
            proveedorId,
            servicioAlternoId
        );
        jdbc.update(
            "INSERT INTO horario_disponible (agenda_id, dia_semana, hora_inicio, hora_fin, duracion_slot_min) VALUES (?, ?, '08:00', '12:00', 30)",
            agendaAlternaId,
            fechaHorario.getDayOfWeek().getValue() - 1
        );
    }

    public Integer recursoDeReserva(String nombreUsuario) {
        return jdbc.queryForObject(
            "SELECT recurso_id FROM reserva WHERE id = ?",
            Integer.class,
            idDeReserva(nombreUsuario)
        );
    }

    private Integer insertarRecurso(String nombre, boolean activo) {
        return jdbc.queryForObject(
            "INSERT INTO recurso (proveedor_id, nombre, tipo, activo) VALUES (?, ?, 'silla', ?) RETURNING id",
            Integer.class,
            proveedorId,
            nombre,
            activo
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

    public Integer servicioId() {
        return servicioId;
    }

    public Integer recursoId() {
        return recursoId;
    }

    public Integer servicioAlternoId() {
        return servicioAlternoId;
    }

    public LocalDate fechaHorario() {
        return fechaHorario;
    }

    public Integer crearReserva(String nombreUsuario, LocalTime hora, String estado) {
        Integer id = jdbc.queryForObject(
            "INSERT INTO reserva (usuario_id, horario_id, servicio_id, recurso_id, fecha, hora, hora_fin, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Integer.class,
            idDeUsuario(nombreUsuario),
            horarioId,
            servicioId,
            recursoId,
            fechaHorario,
            hora,
            hora.plusMinutes(30),
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

    public int contarRecursosDistintosDeReservasActivas() {
        return contar(
            "SELECT count(DISTINCT recurso_id) FROM reserva WHERE horario_id = ? AND fecha = ? AND estado = 'activa'",
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
