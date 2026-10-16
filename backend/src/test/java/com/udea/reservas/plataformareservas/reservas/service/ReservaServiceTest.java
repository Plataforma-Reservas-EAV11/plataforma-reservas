package com.udea.reservas.plataformareservas.reservas.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.repository.IReservaRepository;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    // Jueves 15 de octubre de 2026, 10:00 en la zona del negocio
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 15, 10, 0);

    // 2099-01-01 es jueves: día de la semana 3 (0 = lunes ... 6 = domingo)
    private static final LocalDate FECHA_FUTURA = LocalDate.of(2099, 1, 1);
    private static final int JUEVES = 3;

    @Mock
    private IReservaRepository reservaRepository;

    @Mock
    private UserRepository usuarioRepository;

    @Mock
    private IHorarioDisponibleRepository horarioDisponibleRepository;

    private ReservaServiceImpl reservaServiceImpl;

    @BeforeEach
    void setUp() {
        Clock relojFijo = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        reservaServiceImpl = new ReservaServiceImpl(
            reservaRepository,
            usuarioRepository,
            horarioDisponibleRepository,
            relojFijo
        );
    }

    @Test
    @DisplayName("Crea una reserva y guarda la entidad cuando los datos son válidos")
    void crearReservaValidaGuardaReserva() {
        // Arrange
        User usuario = crearUsuario(1);
        HorarioDisponible horario = crearHorario(true, JUEVES);
        LocalTime hora = LocalTime.of(10, 30);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(horarioDisponibleRepository.findById(10)).thenReturn(
            Optional.of(horario)
        );
        when(
            reservaRepository.existsByHorario_IdAndFechaAndHoraAndEstadoIgnoreCase(
                10,
                FECHA_FUTURA,
                hora,
                "activa"
            )
        ).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> {
            Reserva reserva = invocation.getArgument(0);
            reserva.setId(200);
            return reserva;
        });

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora);

        // Assert
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        Reserva guardada = captor.getValue();
        assertEquals(200, resultado.getId());
        assertSame(usuario, guardada.getUsuario());
        assertSame(horario, guardada.getHorario());
        assertEquals(FECHA_FUTURA, guardada.getFecha());
        assertEquals(hora, guardada.getHora());
        assertEquals("activa", guardada.getEstado());
    }

    @Test
    @DisplayName("Rechaza crear una reserva con un usuario inexistente")
    void crearReservaConUsuarioInexistenteDebeFallar() {
        // Arrange
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());
        LocalTime hora = LocalTime.of(10, 30);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(99, 10, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals("El usuario no existe.", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(horarioDisponibleRepository, never()).findById(any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza crear una reserva con un horario inexistente")
    void crearReservaConHorarioInexistenteDebeFallar() {
        // Arrange
        when(usuarioRepository.findById(1)).thenReturn(
            Optional.of(crearUsuario(1))
        );
        when(horarioDisponibleRepository.findById(9999)).thenReturn(
            Optional.empty()
        );
        LocalTime hora = LocalTime.of(10, 30);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 9999, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals("El horario no existe.", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza crear una reserva si la agenda está inactiva")
    void crearReservaConAgendaInactivaDebeFallar() {
        // Arrange
        when(usuarioRepository.findById(1)).thenReturn(
            Optional.of(crearUsuario(1))
        );
        when(horarioDisponibleRepository.findById(10)).thenReturn(
            Optional.of(crearHorario(false, JUEVES))
        );
        LocalTime hora = LocalTime.of(10, 30);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals(
            "Este horario no está habilitado por el proveedor.",
            exception.getMessage()
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza crear una reserva con fecha y hora pasadas")
    void crearReservaConFechaPasadaDebeFallar() {
        // Arrange
        prepararDatosReservaValida(5);
        LocalDate fecha = LocalDate.of(2000, 1, 1);
        LocalTime hora = LocalTime.of(10, 30);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, fecha, hora)
        );

        // Assert
        assertEquals(
            "No se puede reservar en una fecha u hora que ya pasó.",
            exception.getMessage()
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza reservar hoy un minuto antes de la hora actual (valor límite)")
    void crearReservaUnMinutoAntesDeAhoraDebeFallar() {
        // Arrange
        prepararDatosReservaValida(JUEVES);
        LocalDate hoy = AHORA.toLocalDate();
        LocalTime unMinutoAntes = AHORA.toLocalTime().minusMinutes(1);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, hoy, unMinutoAntes)
        );

        // Assert
        assertEquals(
            "No se puede reservar en una fecha u hora que ya pasó.",
            exception.getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Acepta reservar hoy un minuto después de la hora actual (valor límite)")
    void crearReservaUnMinutoDespuesDeAhoraDebeCrear() {
        // Arrange
        prepararDatosReservaValida(JUEVES);
        LocalDate hoy = AHORA.toLocalDate();
        LocalTime unMinutoDespues = AHORA.toLocalTime().plusMinutes(1);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, hoy, unMinutoDespues);

        // Assert
        assertEquals(unMinutoDespues, resultado.getHora());
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Rechaza una fecha que no corresponde al día de la semana del horario")
    void crearReservaEnOtroDiaDeLaSemanaDebeFallar() {
        // Arrange
        prepararDatosReservaValida(1);
        LocalTime hora = LocalTime.of(10, 30);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals(
            "La fecha seleccionada no corresponde al día habilitado para este horario.",
            exception.getMessage()
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Interpreta el domingo como día 6 de la semana")
    void crearReservaEnDomingoUsaDiaSeis() {
        // Arrange
        prepararDatosReservaValida(6);
        LocalDate domingo = LocalDate.of(2099, 1, 4);
        LocalTime hora = LocalTime.of(10, 0);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, domingo, hora);

        // Assert
        assertEquals(domingo, resultado.getFecha());
    }

    @Test
    @DisplayName("Interpreta el lunes como día 0 de la semana")
    void crearReservaEnLunesUsaDiaCero() {
        // Arrange
        prepararDatosReservaValida(0);
        LocalDate lunes = LocalDate.of(2099, 1, 5);
        LocalTime hora = LocalTime.of(10, 0);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, lunes, hora);

        // Assert
        assertEquals(lunes, resultado.getFecha());
    }

    @Test
    @DisplayName("Rechaza una hora anterior al inicio del horario")
    void crearReservaAntesDelInicioDebeFallar() {
        // Arrange
        prepararDatosReservaValida(JUEVES);
        LocalTime hora = LocalTime.of(9, 59);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals(
            "La hora seleccionada está fuera del rango de este horario.",
            exception.getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Acepta una hora igual al inicio del horario (valor límite)")
    void crearReservaIgualAlInicioDebeCrear() {
        // Arrange
        prepararDatosReservaValida(JUEVES);
        LocalTime hora = LocalTime.of(10, 0);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora);

        // Assert
        assertEquals(hora, resultado.getHora());
    }

    @Test
    @DisplayName("Rechaza una hora igual al fin del horario")
    void crearReservaIgualAlFinDebeFallar() {
        // Arrange
        prepararDatosReservaValida(JUEVES);
        LocalTime hora = LocalTime.of(12, 0);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals(
            "La hora seleccionada está fuera del rango de este horario.",
            exception.getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza una hora posterior al fin del horario")
    void crearReservaDespuesDelFinDebeFallar() {
        // Arrange
        prepararDatosReservaValida(JUEVES);
        LocalTime hora = LocalTime.of(12, 1);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals(
            "La hora seleccionada está fuera del rango de este horario.",
            exception.getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza crear una reserva si el horario ya está reservado")
    void crearReservaConHorarioOcupadoDebeFallar() {
        // Arrange
        LocalTime hora = LocalTime.of(10, 30);
        prepararDatosReservaValida(JUEVES);
        when(
            reservaRepository.existsByHorario_IdAndFechaAndHoraAndEstadoIgnoreCase(
                10,
                FECHA_FUTURA,
                hora,
                "activa"
            )
        ).thenReturn(true);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals(
            "El recurso no está disponible en la fecha y hora seleccionadas.",
            exception.getMessage()
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Traduce la violación del índice único (reserva simultánea) a 409")
    void crearReservaSimultaneaDebeResponderConflicto() {
        // Arrange
        LocalTime hora = LocalTime.of(10, 30);
        prepararDatosReservaValida(JUEVES);
        when(reservaRepository.save(any(Reserva.class))).thenThrow(
            new DataIntegrityViolationException("ux_reserva_horario_fecha_activa")
        );

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals(
            "El recurso no está disponible en la fecha y hora seleccionadas.",
            exception.getMessage()
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    @DisplayName("Cancela una reserva activa de su propietario")
    void cancelarReservaActivaDeberiaActualizarEstado() {
        // Arrange
        Reserva reserva = crearReserva(100, 1, "activa");
        when(reservaRepository.findById(100)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(
            invocation -> invocation.getArgument(0)
        );

        // Act
        Reserva resultado = reservaServiceImpl.cancelarReserva(100, 1);

        // Assert
        assertEquals("cancelada", resultado.getEstado());
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("Rechaza cancelar una reserva inexistente")
    void cancelarReservaInexistenteDebeFallar() {
        // Arrange
        when(reservaRepository.findById(999)).thenReturn(Optional.empty());

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.cancelarReserva(999, 1)
        );

        // Assert
        assertEquals("La reserva no existe.", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza cancelar una reserva de otro usuario")
    void cancelarReservaDeOtroUsuarioDebeFallar() {
        // Arrange
        Reserva reserva = crearReserva(101, 1, "activa");
        when(reservaRepository.findById(101)).thenReturn(Optional.of(reserva));

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.cancelarReserva(101, 2)
        );

        // Assert
        assertEquals(
            "No puedes cancelar una reserva que no te pertenece.",
            exception.getMessage()
        );
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("activa", reserva.getEstado());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Valida la propiedad antes que el estado para no revelar datos de reservas ajenas")
    void cancelarReservaAjenaYaCanceladaDebeResponderProhibido() {
        // Arrange
        Reserva reserva = crearReserva(105, 1, "cancelada");
        when(reservaRepository.findById(105)).thenReturn(Optional.of(reserva));

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.cancelarReserva(105, 2)
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza cancelar una reserva ya cancelada")
    void cancelarReservaYaCanceladaDebeFallar() {
        // Arrange
        Reserva reserva = crearReserva(102, 1, "cancelada");
        when(reservaRepository.findById(102)).thenReturn(Optional.of(reserva));

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.cancelarReserva(102, 1)
        );

        // Assert
        assertEquals("La reserva ya está cancelada.", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza cancelar una reserva completada")
    void cancelarReservaCompletadaDebeFallar() {
        // Arrange
        Reserva reserva = crearReserva(103, 1, "completada");
        when(reservaRepository.findById(103)).thenReturn(Optional.of(reserva));

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.cancelarReserva(103, 1)
        );

        // Assert
        assertEquals(
            "La reserva ya fue completada y no puede cancelarse.",
            exception.getMessage()
        );
        assertEquals("completada", reserva.getEstado());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza cancelar una reserva con estado desconocido")
    void cancelarReservaConEstadoDesconocidoDebeFallar() {
        // Arrange
        Reserva reserva = crearReserva(104, 1, "pendiente");
        when(reservaRepository.findById(104)).thenReturn(Optional.of(reserva));

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.cancelarReserva(104, 1)
        );

        // Assert
        assertEquals("La reserva no está activa.", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    private void prepararDatosReservaValida(int diaSemana) {
        when(usuarioRepository.findById(1)).thenReturn(
            Optional.of(crearUsuario(1))
        );
        when(horarioDisponibleRepository.findById(10)).thenReturn(
            Optional.of(crearHorario(true, diaSemana))
        );
    }

    private User crearUsuario(int usuarioId) {
        User usuario = new User();
        usuario.setId(usuarioId);
        usuario.setName("Ana");
        usuario.setEmail("ana@test.com");
        usuario.setPasswordHash("hash");
        usuario.setRole("usuario");
        return usuario;
    }

    private HorarioDisponible crearHorario(boolean agendaActiva, int diaSemana) {
        Agenda agenda = new Agenda();
        agenda.setActiva(agendaActiva);

        HorarioDisponible horario = new HorarioDisponible();
        horario.setId(10);
        horario.setAgenda(agenda);
        horario.setDiaSemana(diaSemana);
        horario.setHoraInicio(LocalTime.of(10, 0));
        horario.setHoraFin(LocalTime.of(12, 0));
        horario.setDuracionSlotMin(30);
        return horario;
    }

    private Reserva crearReserva(int reservaId, int usuarioId, String estado) {
        return Reserva.builder()
            .id(reservaId)
            .usuario(crearUsuario(usuarioId))
            .horario(crearHorario(true, JUEVES))
            .fecha(FECHA_FUTURA)
            .hora(LocalTime.of(10, 30))
            .estado(estado)
            .build();
    }
}
