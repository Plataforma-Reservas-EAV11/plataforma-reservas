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
import com.udea.reservas.plataformareservas.usuarios.model.Usuario;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HorarioDisponibleServiceTest {

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
        Usuario usuario = Usuario.builder()
            .id(1)
            .nombre("Juan Pérez")
            .email("juan@test.com")
            .passwordHash("hash123")
            .rol("proveedor")
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

        Agenda agenda = Agenda.builder()
            .id(1)
            .proveedor(proveedor)
            .recurso(recurso)
            .nombre("Agenda Principal")
            .activa(true)
            .build();

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

        HorarioResponse resultado = horarioDisponibleService.crearHorario(
            agenda.getId(),
            request
        );

        assertThat(resultado)
            .isNotNull()
            .extracting(
                HorarioResponse::agendaId,
                HorarioResponse::diaSemana,
                HorarioResponse::horaInicio,
                HorarioResponse::horaFin,
                HorarioResponse::duracionSlotMin
            )
            .containsExactly(
                agenda.getId(),
                request.diaSemana(),
                request.horaInicio(),
                request.horaFin(),
                request.duracionSlotMin()
            );

        verify(horarioDisponibleRepository).save(any(HorarioDisponible.class));
    }

    @Test
    @DisplayName("Lanzar excepción cuando la agenda no existe")
    void lanzaExcepcionAgendaNoExiste() {
        CrearHorarioRequest request = new CrearHorarioRequest(
            2,
            LocalTime.of(10, 30),
            LocalTime.of(12, 00),
            90
        );

        when(agendaRepository.findById(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            horarioDisponibleService.crearHorario(7, request)
        )
            .isInstanceOf(AgendaException.class)
            .hasMessage("La agenda con ID " + 7 + " no existe.");

        verify(agendaRepository).findById(7);
        verify(horarioDisponibleRepository, never()).save(any());
    }
}
