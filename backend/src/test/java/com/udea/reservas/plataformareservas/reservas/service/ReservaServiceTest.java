package com.udea.reservas.plataformareservas.reservas.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.repository.IReservaRepository;
import com.udea.reservas.plataformareservas.usuarios.model.Usuario;
import com.udea.reservas.plataformareservas.usuarios.repository.IUsuarioRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private IReservaRepository reservaRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IHorarioDisponibleRepository horarioDisponibleRepository;

    @InjectMocks
    private ReservaServiceImpl reservaServiceImpl;

    @Test
    @DisplayName("Crea una reserva y guarda la entidad cuando los datos son válidos")
    void crearReservaValidaGuardaReserva() {
        // Arrange
        Usuario usuario = crearUsuario(1);
        HorarioDisponible horario = crearHorario(true);
        LocalDate fecha = LocalDate.of(2099, 1, 1);
        LocalTime hora = LocalTime.of(10, 30);
        Reserva reservaGuardada = Reserva.builder()
            .id(200)
            .usuario(usuario)
            .horario(horario)
            .fecha(fecha)
            .hora(hora)
            .estado("activa")
            .build();

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(horarioDisponibleRepository.findById(10)).thenReturn(
            Optional.of(horario)
        );
        when(
            reservaRepository.existsByHorario_IdAndFechaAndEstado(
                10,
                fecha,
                "activa"
            )
        ).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenReturn(
            reservaGuardada
        );

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, fecha, hora);

        // Assert
        assertEquals(reservaGuardada, resultado);
        verify(reservaRepository).save(any(Reserva.class));
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

    @Test
    @DisplayName("Rechaza crear una reserva si la agenda está inactiva")
    void crearReservaConAgendaInactivaDebeFallar() {
        // Arrange
        Usuario usuario = crearUsuario(1);
        HorarioDisponible horario = crearHorario(false);
        LocalDate fecha = LocalDate.of(2099, 1, 1);
        LocalTime hora = LocalTime.of(10, 30);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(horarioDisponibleRepository.findById(10)).thenReturn(
            Optional.of(horario)
        );

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, fecha, hora)
        );

        // Assert
        assertEquals(
            "Este horario no está habilitado por el proveedor.",
            exception.getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza crear una reserva con fecha y hora pasadas")
    void crearReservaConFechaPasadaDebeFallar() {
        // Arrange
        prepararDatosReservaValida();
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
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza una hora anterior al inicio del horario")
    void crearReservaAntesDelInicioDebeFallar() {
        // Arrange
        prepararDatosReservaValida();
        LocalDate fecha = LocalDate.of(2099, 1, 1);
        LocalTime hora = LocalTime.of(9, 59);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, fecha, hora)
        );

        // Assert
        assertEquals(
            "La hora seleccionada está fuera del rango de este horario.",
            exception.getMessage()
        );
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza una hora igual al fin del horario")
    void crearReservaIgualAlFinDebeFallar() {
        // Arrange
        prepararDatosReservaValida();
        LocalDate fecha = LocalDate.of(2099, 1, 1);
        LocalTime hora = LocalTime.of(12, 0);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, fecha, hora)
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
        prepararDatosReservaValida();
        LocalDate fecha = LocalDate.of(2099, 1, 1);
        LocalTime hora = LocalTime.of(12, 1);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, fecha, hora)
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
        LocalDate fecha = LocalDate.of(2099, 1, 1);
        LocalTime hora = LocalTime.of(10, 30);
        prepararDatosReservaValida();
        when(
            reservaRepository.existsByHorario_IdAndFechaAndEstado(
                10,
                fecha,
                "activa"
            )
        ).thenReturn(true);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, fecha, hora)
        );

        // Assert
        assertEquals(
            "Este horario ya está reservado para esa fecha.",
            exception.getMessage()
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cancela una reserva activa de su propietario")
    void cancelarReservaActivaDeberiaActualizarEstado() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNombre("Ana");
        usuario.setEmail("ana@test.com");
        usuario.setPasswordHash("hash");
        usuario.setRol("cliente");

        HorarioDisponible horario = new HorarioDisponible();
        horario.setId(10);

        Reserva reserva = Reserva.builder()
            .id(100)
            .usuario(usuario)
            .horario(horario)
            .fecha(LocalDate.of(2099, 1, 1))
            .hora(LocalTime.of(10, 0))
            .estado("activa")
            .build();

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
    @DisplayName("Rechaza cancelar una reserva de otro usuario")
    void cancelarReservaDeOtroUsuarioDebeFallar() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setId(1);

        Reserva reserva = Reserva.builder()
            .id(101)
            .usuario(usuario)
            .horario(new HorarioDisponible())
            .fecha(LocalDate.of(2099, 1, 2))
            .hora(LocalTime.of(9, 30))
            .estado("activa")
            .build();

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
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza cancelar una reserva ya cancelada")
    void cancelarReservaYaCanceladaDebeFallar() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setId(1);

        Reserva reserva = Reserva.builder()
            .id(102)
            .usuario(usuario)
            .horario(new HorarioDisponible())
            .fecha(LocalDate.of(2099, 1, 3))
            .hora(LocalTime.of(11, 0))
            .estado("cancelada")
            .build();

        when(reservaRepository.findById(102)).thenReturn(Optional.of(reserva));

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.cancelarReserva(102, 1)
        );

        // Assert
        assertEquals("La reserva ya está cancelada.", exception.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza cancelar una reserva completada")
    void cancelarReservaCompletadaDebeFallar() {
        // Arrange
        Usuario usuario = new Usuario();
        usuario.setId(1);

        Reserva reserva = Reserva.builder()
            .id(103)
            .usuario(usuario)
            .horario(new HorarioDisponible())
            .fecha(LocalDate.of(2099, 1, 4))
            .hora(LocalTime.of(12, 0))
            .estado("completada")
            .build();

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
        verify(reservaRepository, never()).save(any());
    }

    private void prepararDatosReservaValida() {
        when(usuarioRepository.findById(1)).thenReturn(
            Optional.of(crearUsuario(1))
        );
        when(horarioDisponibleRepository.findById(10)).thenReturn(
            Optional.of(crearHorario(true))
        );
    }

    private Usuario crearUsuario(int usuarioId) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNombre("Ana");
        usuario.setEmail("ana@test.com");
        usuario.setPasswordHash("hash");
        usuario.setRol("cliente");
        return usuario;
    }

    private HorarioDisponible crearHorario(boolean agendaActiva) {
        com.udea.reservas.plataformareservas.agendas.model.Agenda agenda =
            new com.udea.reservas.plataformareservas.agendas.model.Agenda();
        agenda.setActiva(agendaActiva);

        HorarioDisponible horario = new HorarioDisponible();
        horario.setId(10);
        horario.setAgenda(agenda);
        horario.setHoraInicio(LocalTime.of(10, 0));
        horario.setHoraFin(LocalTime.of(12, 0));
        return horario;
    }

    private Reserva crearReserva(int reservaId, int usuarioId, String estado) {
        return Reserva.builder()
            .id(reservaId)
            .usuario(crearUsuario(usuarioId))
            .horario(crearHorario(true))
            .fecha(LocalDate.of(2099, 1, 1))
            .hora(LocalTime.of(10, 30))
            .estado(estado)
            .build();
    }
}
