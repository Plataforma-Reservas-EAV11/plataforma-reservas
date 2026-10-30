package com.udea.reservas.plataformareservas.agendas.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.exception.AgendaException;
import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class HorarioDisponibleServiceTest {

    // Usuario dueño del proveedor de la agenda que arma crearAgenda()
    private static final int USUARIO_DUENIO = 1;

    @Mock
    private IAgendaRepository agendaRepository;

    @Mock
    private IHorarioDisponibleRepository horarioDisponibleRepository;

    @InjectMocks
    private HorarioDisponibleServiceImpl horarioDisponibleService;

    @Test
    @DisplayName(
        "Definir bloque de disponibilidad válido con rango de horas correcto"
    )
    void defineBloqueDisponibilidadValido() {
        // Arrange
        Agenda agenda = crearAgenda();
        HorarioDisponible horario = HorarioDisponible.builder()
            .id(1)
            .agenda(agenda)
            .diaSemana(2)
            .horaInicio(LocalTime.of(10, 30))
            .horaFin(LocalTime.of(12, 00))
            .duracionSlotMin(90)
            .build();

        when(agendaRepository.findById(agenda.getId())).thenReturn(
            Optional.of(agenda)
        );
        when(
            horarioDisponibleRepository.save(any(HorarioDisponible.class))
        ).thenReturn(horario);

        CrearHorarioRequest request = new CrearHorarioRequest(
            horario.getDiaSemana(),
            horario.getHoraInicio(),
            horario.getHoraFin(),
            horario.getDuracionSlotMin()
        );

        // Act
        HorarioResponse resultado = horarioDisponibleService.crearHorario(
            agenda.getId(),
            USUARIO_DUENIO,
            request
        );

        // Assert
        assertThat(resultado)
            .isNotNull()
            .extracting(
                HorarioResponse::id,
                HorarioResponse::agendaId,
                HorarioResponse::diaSemana,
                HorarioResponse::horaInicio,
                HorarioResponse::horaFin,
                HorarioResponse::duracionSlotMin
            )
            .containsExactly(
                1,
                agenda.getId(),
                request.diaSemana(),
                request.horaInicio(),
                request.horaFin(),
                request.duracionSlotMin()
            );

        ArgumentCaptor<HorarioDisponible> captor =
            ArgumentCaptor.forClass(HorarioDisponible.class);
        verify(horarioDisponibleRepository).save(captor.capture());
        assertThat(captor.getValue().getAgenda()).isSameAs(agenda);
    }

    @Test
    @DisplayName("Lanzar excepción 404 cuando la agenda no existe")
    void lanzaExcepcionAgendaNoExiste() {
        // Arrange
        CrearHorarioRequest request = new CrearHorarioRequest(
            2,
            LocalTime.of(10, 30),
            LocalTime.of(12, 00),
            90
        );
        when(agendaRepository.findById(7)).thenReturn(Optional.empty());

        // Act
        Throwable thrown = catchThrowable(() ->
            horarioDisponibleService.crearHorario(7, USUARIO_DUENIO, request)
        );

        // Assert
        assertThat(thrown)
            .isInstanceOf(AgendaException.class)
            .hasMessage("La agenda con ID " + 7 + " no existe.");
        assertThat(((AgendaException) thrown).getStatus())
            .isEqualTo(HttpStatus.NOT_FOUND);
        verify(horarioDisponibleRepository, never()).save(any());
    }

    @ParameterizedTest(name = "Rechaza el rango {0} - {1}")
    @CsvSource({ "10:00, 10:00", "10:00, 09:59", "14:00, 08:00" })
    @DisplayName("Rechaza bloques cuya hora de fin no es posterior a la de inicio")
    void rechazaRangoDeHorasInvalido(LocalTime horaInicio, LocalTime horaFin) {
        // Arrange
        Agenda agenda = crearAgenda();
        when(agendaRepository.findById(1)).thenReturn(Optional.of(agenda));
        CrearHorarioRequest request = new CrearHorarioRequest(
            1,
            horaInicio,
            horaFin,
            30
        );

        // Act
        Throwable thrown = catchThrowable(() ->
            horarioDisponibleService.crearHorario(1, USUARIO_DUENIO, request)
        );

        // Assert
        assertThat(thrown)
            .isInstanceOf(AgendaException.class)
            .hasMessage(
                "El rango de horas no es válido. La hora de fin debe ser posterior a la de inicio."
            );
        assertThat(((AgendaException) thrown).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(horarioDisponibleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Acepta el valor límite de un minuto de duración")
    void aceptaBloqueDeUnMinuto() {
        // Arrange
        Agenda agenda = crearAgenda();
        when(agendaRepository.findById(1)).thenReturn(Optional.of(agenda));
        when(horarioDisponibleRepository.save(any(HorarioDisponible.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        CrearHorarioRequest request = new CrearHorarioRequest(
            1,
            LocalTime.of(10, 0),
            LocalTime.of(10, 1),
            1
        );

        // Act
        HorarioResponse resultado = horarioDisponibleService.crearHorario(1, USUARIO_DUENIO, request);

        // Assert
        assertThat(resultado.horaFin()).isEqualTo(LocalTime.of(10, 1));
        verify(horarioDisponibleRepository).save(any(HorarioDisponible.class));
    }

    @Test
    @DisplayName("Rechaza un bloque que se solapa con otro de la misma agenda y día")
    void rechazaBloqueSolapado() {
        // Arrange
        Agenda agenda = crearAgenda();
        when(agendaRepository.findById(1)).thenReturn(Optional.of(agenda));
        when(
            horarioDisponibleRepository.existsByAgenda_IdAndDiaSemanaAndHoraInicioLessThanAndHoraFinGreaterThan(
                1,
                1,
                LocalTime.of(13, 0),
                LocalTime.of(11, 0)
            )
        ).thenReturn(true);
        CrearHorarioRequest request = new CrearHorarioRequest(
            1,
            LocalTime.of(11, 0),
            LocalTime.of(13, 0),
            30
        );

        // Act
        Throwable thrown = catchThrowable(() ->
            horarioDisponibleService.crearHorario(1, USUARIO_DUENIO, request)
        );

        // Assert
        assertThat(thrown)
            .isInstanceOf(AgendaException.class)
            .hasMessage(
                "El horario ya tiene disponibilidad definida. No se pueden solapar bloques."
            );
        assertThat(((AgendaException) thrown).getStatus())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(horarioDisponibleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Consulta el solapamiento con la regla inicioExistente < finNuevo y finExistente > inicioNuevo")
    void consultaSolapamientoConLimitesEstrictos() {
        // Arrange
        Agenda agenda = crearAgenda();
        when(agendaRepository.findById(1)).thenReturn(Optional.of(agenda));
        when(horarioDisponibleRepository.save(any(HorarioDisponible.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        CrearHorarioRequest request = new CrearHorarioRequest(
            3,
            LocalTime.of(12, 0),
            LocalTime.of(13, 0),
            30
        );

        // Act
        horarioDisponibleService.crearHorario(1, USUARIO_DUENIO, request);

        // Assert
        verify(horarioDisponibleRepository)
            .existsByAgenda_IdAndDiaSemanaAndHoraInicioLessThanAndHoraFinGreaterThan(
                1,
                3,
                LocalTime.of(13, 0),
                LocalTime.of(12, 0)
            );
    }

    @Test
    @DisplayName("DEF-01: rechaza con 403 definir horarios en la agenda de otro proveedor")
    void rechazaAgendaDeOtroProveedor() {
        // Arrange
        Agenda agenda = crearAgenda();
        int otroUsuario = USUARIO_DUENIO + 1;
        when(agendaRepository.findById(agenda.getId())).thenReturn(
            Optional.of(agenda)
        );
        CrearHorarioRequest request = new CrearHorarioRequest(
            2,
            LocalTime.of(10, 30),
            LocalTime.of(12, 0),
            90
        );

        // Act
        Throwable thrown = catchThrowable(() ->
            horarioDisponibleService.crearHorario(agenda.getId(), otroUsuario, request)
        );

        // Assert
        assertThat(thrown)
            .isInstanceOf(AgendaException.class)
            .hasMessage("No puedes modificar una agenda que no te pertenece.");
        assertThat(((AgendaException) thrown).getStatus())
            .isEqualTo(HttpStatus.FORBIDDEN);
        verify(horarioDisponibleRepository, never()).save(any());
    }

    private Agenda crearAgenda() {
        User usuario = User.builder()
            .id(1)
            .name("Juan Pérez")
            .email("juan@test.com")
            .passwordHash("hash123")
            .role("proveedor")
            .build();

        Proveedor proveedor = Proveedor.builder()
            .id(1)
            .usuario(usuario)
            .nombreNegocio("Barbería Style")
            .telefono("3001234567")
            .build();

        Recurso recurso = Recurso.builder()
            .id(1)
            .proveedor(proveedor)
            .nombre("Silla 1")
            .tipo("silla")
            .activo(true)
            .build();

        return Agenda.builder()
            .id(1)
            .proveedor(proveedor)
            .recurso(recurso)
            .nombre("Agenda Principal")
            .activa(true)
            .build();
    }
}
