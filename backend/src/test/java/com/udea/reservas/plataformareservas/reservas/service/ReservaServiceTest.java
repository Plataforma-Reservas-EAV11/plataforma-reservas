package com.udea.reservas.plataformareservas.reservas.service;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.HorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.repository.ReservaRepository;
import com.udea.reservas.plataformareservas.usuarios.model.Usuario;
import com.udea.reservas.plataformareservas.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HorarioDisponibleRepository horarioDisponibleRepository;

    @InjectMocks
    private ReservaService reservaService;

    @Test
    void cancelarReservaActivaDeberiaActualizarEstado() {
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
                .fecha(LocalDate.now().plusDays(1))
                .hora(LocalTime.of(10, 0))
                .estado("activa")
                .build();

        when(reservaRepository.findById(100)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva resultado = reservaService.cancelarReserva(100, 1);

        assertEquals("cancelada", resultado.getEstado());
        verify(reservaRepository).save(reserva);
    }

    @Test
    void cancelarReservaDeOtroUsuarioDebeFallar() {
        Usuario usuario = new Usuario();
        usuario.setId(1);

        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(2);

        Reserva reserva = Reserva.builder()
                .id(101)
                .usuario(usuario)
                .horario(new HorarioDisponible())
                .fecha(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(9, 30))
                .estado("activa")
                .build();

        when(reservaRepository.findById(101)).thenReturn(Optional.of(reserva));

        ReservaException exception = assertThrows(ReservaException.class,
                () -> reservaService.cancelarReserva(101, 2));

        assertEquals("No puedes cancelar una reserva que no te pertenece.", exception.getMessage());
    }

    @Test
    void cancelarReservaYaCanceladaDebeFallar() {
        Usuario usuario = new Usuario();
        usuario.setId(1);

        Reserva reserva = Reserva.builder()
                .id(102)
                .usuario(usuario)
                .horario(new HorarioDisponible())
                .fecha(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(11, 0))
                .estado("cancelada")
                .build();

        when(reservaRepository.findById(102)).thenReturn(Optional.of(reserva));

        ReservaException exception = assertThrows(ReservaException.class,
                () -> reservaService.cancelarReserva(102, 1));

        assertEquals("La reserva ya está cancelada.", exception.getMessage());
    }

    @Test
    void cancelarReservaCompletadaDebeFallar() {
        Usuario usuario = new Usuario();
        usuario.setId(1);

        Reserva reserva = Reserva.builder()
                .id(103)
                .usuario(usuario)
                .horario(new HorarioDisponible())
                .fecha(LocalDate.now().plusDays(3))
                .hora(LocalTime.of(12, 0))
                .estado("completada")
                .build();

        when(reservaRepository.findById(103)).thenReturn(Optional.of(reserva));

        ReservaException exception = assertThrows(ReservaException.class,
                () -> reservaService.cancelarReserva(103, 1));

        assertEquals("La reserva ya fue completada y no puede cancelarse.", exception.getMessage());
    }
}
