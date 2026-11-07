package com.udea.reservas.plataformareservas.reservas.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.repository.IReservaRepository;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.recursos.repository.RecursoRepository;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import com.udea.reservas.plataformareservas.servicios.repository.IServicioRepository;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    @Mock
    private IAgendaRepository agendaRepository;

    @Mock
    private IServicioRepository servicioRepository;

    @Mock
    private RecursoRepository recursoRepository;

    private ReservaServiceImpl reservaServiceImpl;

    @BeforeEach
    void setUp() {
        Clock relojFijo = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        reservaServiceImpl = new ReservaServiceImpl(
            reservaRepository,
            usuarioRepository,
            horarioDisponibleRepository,
            agendaRepository,
            servicioRepository,
            recursoRepository,
            relojFijo
        );
    }

    @Test
    @DisplayName("Crea una reserva y guarda la entidad cuando los datos son válidos")
    void crearReservaValidaGuardaReserva() {
        // Arrange
        LocalTime hora = LocalTime.of(10, 30);
        prepararDatosReservaValida(JUEVES);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(invocation -> {
            Reserva reserva = invocation.getArgument(0);
            reserva.setId(200);
            return reserva;
        });

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora);

        // Assert
        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).saveAndFlush(captor.capture());
        Reserva guardada = captor.getValue();
        assertEquals(200, resultado.getId());
        assertEquals(1, guardada.getUsuario().getId());
        assertEquals(10, guardada.getHorario().getId());
        assertEquals(10, guardada.getServicio().getId());
        assertEquals(30, guardada.getRecurso().getId());
        assertEquals(FECHA_FUTURA, guardada.getFecha());
        assertEquals(hora, guardada.getHora());
        assertEquals(LocalTime.of(11, 0), guardada.getHoraFin());
        assertEquals("activa", guardada.getEstado());
        assertEquals(AHORA, guardada.getCreatedAt());
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
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Rechaza crear una reserva con un servicio inexistente")
    void crearReservaConServicioInexistenteDebeFallar() {
        // Arrange
        when(usuarioRepository.findById(1)).thenReturn(
            Optional.of(crearUsuario(1))
        );
        when(servicioRepository.findActivoForUpdate(9999)).thenReturn(
            Optional.empty()
        );
        LocalTime hora = LocalTime.of(10, 30);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 9999, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals("El servicio no existe.", exception.getMessage());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Rechaza crear una reserva si la agenda está inactiva")
    void crearReservaConAgendaInactivaDebeFallar() {
        // Arrange
        prepararDatosReservaValida(JUEVES);
        Agenda agenda = crearAgenda(false);
        when(agendaRepository.findByServicio_Id(10)).thenReturn(Optional.of(agenda));
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
        verify(reservaRepository, never()).saveAndFlush(any());
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
        verify(reservaRepository, never()).saveAndFlush(any());
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
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Acepta reservar hoy un minuto después de la hora actual (valor límite)")
    void crearReservaUnMinutoDespuesDeAhoraDebeCrear() {
        // Arrange
        prepararDatosReservaValida(JUEVES);
        LocalDate hoy = AHORA.toLocalDate();
        LocalTime unMinutoDespues = AHORA.toLocalTime().plusMinutes(1);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, hoy, unMinutoDespues);

        // Assert
        assertEquals(unMinutoDespues, resultado.getHora());
        verify(reservaRepository).saveAndFlush(any(Reserva.class));
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
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Interpreta el domingo como día 6 de la semana")
    void crearReservaEnDomingoUsaDiaSeis() {
        // Arrange
        prepararDatosReservaValida(6);
        LocalDate domingo = LocalDate.of(2099, 1, 4);
        LocalTime hora = LocalTime.of(10, 0);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(invocation ->
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
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, lunes, hora);

        // Assert
        assertEquals(lunes, resultado.getFecha());
    }

    @ParameterizedTest(name = "Rechaza la hora {0} en un horario de 10:00 a 12:00")
    @ValueSource(strings = { "09:59", "12:00", "12:01" })
    @DisplayName("Rechaza horas fuera del rango del horario (antes del inicio, igual al fin y después del fin)")
    void crearReservaFueraDelRangoDebeFallar(LocalTime hora) {
        // Arrange
        prepararDatosReservaValida(JUEVES);

        // Act
        ReservaException exception = assertThrows(ReservaException.class, () ->
            reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora)
        );

        // Assert
        assertEquals("La hora seleccionada está fuera del rango de este horario.", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Acepta una hora igual al inicio del horario (valor límite)")
    void crearReservaIgualAlInicioDebeCrear() {
        // Arrange
        prepararDatosReservaValida(JUEVES);
        LocalTime hora = LocalTime.of(10, 0);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        // Act
        Reserva resultado = reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora);

        // Assert
        assertEquals(hora, resultado.getHora());
    }

    @Test
    @DisplayName("Rechaza crear una reserva si el horario ya está reservado")
    void crearReservaConHorarioOcupadoDebeFallar() {
        // Arrange
        LocalTime hora = LocalTime.of(10, 30);
        prepararDatosReservaValida(JUEVES);
        when(
            reservaRepository.existsReservaActivaSolapada(
                30,
                FECHA_FUTURA,
                hora,
                hora.plusMinutes(30)
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
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Asigna el siguiente recurso compatible cuando el primero tiene un intervalo solapado")
    void crearReservaSeleccionaSiguienteRecursoDisponible() {
        LocalTime hora = LocalTime.of(10, 30);
        prepararDatosReservaValida(JUEVES);
        when(recursoRepository.findActiveCompatibleForUpdate(10, 20)).thenReturn(
            List.of(crearRecurso(30), crearRecurso(31))
        );
        when(
            reservaRepository.existsReservaActivaSolapada(
                30,
                FECHA_FUTURA,
                hora,
                hora.plusMinutes(30)
            )
        ).thenReturn(true);
        when(
            reservaRepository.existsReservaActivaSolapada(
                31,
                FECHA_FUTURA,
                hora,
                hora.plusMinutes(30)
            )
        ).thenReturn(false);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(invocation ->
            invocation.getArgument(0)
        );

        Reserva creada = reservaServiceImpl.crearReserva(1, 10, FECHA_FUTURA, hora);

        assertEquals(31, creada.getRecurso().getId());
    }

    @Test
    @DisplayName("Rechaza la reserva si el servicio no tiene recursos compatibles activos")
    void crearReservaSinRecursosCompatiblesDebeFallar() {
        prepararDatosReservaValida(JUEVES);
        when(recursoRepository.findActiveCompatibleForUpdate(10, 20)).thenReturn(List.of());

        ReservaException exception = assertThrows(
            ReservaException.class,
            () -> reservaServiceImpl.crearReserva(
                1,
                10,
                FECHA_FUTURA,
                LocalTime.of(10, 30)
            )
        );

        assertEquals(
            "El servicio aún no tiene recursos disponibles para reservar.",
            exception.getMessage()
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Traduce la violación de exclusión de intervalo a 409")
    void crearReservaSimultaneaDebeResponderConflicto() {
        // Arrange
        LocalTime hora = LocalTime.of(10, 30);
        prepararDatosReservaValida(JUEVES);
        org.hibernate.exception.ConstraintViolationException violation =
            new org.hibernate.exception.ConstraintViolationException(
                "overlap",
                null,
                "ex_reserva_recurso_intervalo"
            );
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenThrow(
            new DataIntegrityViolationException("constraint violation", violation)
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
    @DisplayName("No convierte otras violaciones de integridad en conflicto de disponibilidad")
    void crearReservaNoOcultaOtrasViolacionesDeIntegridad() {
        prepararDatosReservaValida(JUEVES);
        DataIntegrityViolationException failure =
            new DataIntegrityViolationException("foreign key violation");
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenThrow(failure);

        assertSame(
            failure,
            assertThrows(
                DataIntegrityViolationException.class,
                () -> reservaServiceImpl.crearReserva(
                    1,
                    10,
                    FECHA_FUTURA,
                    LocalTime.of(10, 30)
                )
            )
        );
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
        Servicio servicio = crearServicio();
        Agenda agenda = crearAgenda(true);
        HorarioDisponible horario = crearHorario(agenda, diaSemana);
        when(servicioRepository.findActivoForUpdate(10)).thenReturn(Optional.of(servicio));
        Mockito.lenient()
            .when(agendaRepository.findByServicio_Id(10))
            .thenReturn(Optional.of(agenda));
        Mockito.lenient().when(
            horarioDisponibleRepository.findAllByAgenda_IdAndDiaSemanaOrderByHoraInicioAsc(
                40,
                diaSemana
            )
        ).thenReturn(
            List.of(horario)
        );
        Mockito.lenient()
            .when(recursoRepository.findActiveCompatibleForUpdate(10, 20))
            .thenReturn(List.of(crearRecurso()));
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

    private Servicio crearServicio() {
        Proveedor proveedor = Proveedor.builder().id(20).build();
        return Servicio.builder()
            .id(10)
            .proveedor(proveedor)
            .nombre("Consulta")
            .duracionMinutos(30)
            .build();
    }

    private Agenda crearAgenda(boolean activa) {
        Proveedor proveedor = Proveedor.builder().id(20).build();
        return Agenda.builder()
            .id(40)
            .proveedor(proveedor)
            .servicio(crearServicio())
            .activa(activa)
            .build();
    }

    private Recurso crearRecurso() {
        return crearRecurso(30);
    }

    private Recurso crearRecurso(int recursoId) {
        return Recurso.builder()
            .id(recursoId)
            .proveedor(Proveedor.builder().id(20).build())
            .nombre("Consultorio")
            .activo(true)
            .build();
    }

    private HorarioDisponible crearHorario(Agenda agenda, int diaSemana) {
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
            .horario(crearHorario(crearAgenda(true), JUEVES))
            .fecha(FECHA_FUTURA)
            .hora(LocalTime.of(10, 30))
            .estado(estado)
            .build();
    }
}
