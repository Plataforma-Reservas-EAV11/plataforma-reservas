package com.udea.reservas.plataformareservas.reservas.service;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.repository.IReservaRepository;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import com.udea.reservas.plataformareservas.usuarios.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private static final String ESTADO_ACTIVA = "activa";
    private final IReservaRepository reservaRepository;
    private final UserRepository usuarioRepository;
    private final IHorarioDisponibleRepository horarioDisponibleRepository;

    @Override
    @Transactional
    public Reserva crearReserva(Integer usuarioId, Integer horarioId, LocalDate fecha, LocalTime hora) {
        User usuario = buscarUsuario(usuarioId);
        HorarioDisponible horario = buscarHorario(horarioId);

        validarReglasDeCreacion(horario, fecha, hora);

        Reserva reserva = Reserva.builder()
                .usuario(usuario)
                .horario(horario)
                .fecha(fecha)
                .hora(hora)
                .estado(ESTADO_ACTIVA)
                .build();

        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva cancelarReserva(Integer reservaId, Integer usuarioId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaException("La reserva no existe.", HttpStatus.NOT_FOUND));

        validarEstadoParaCancelar(reserva);
        validarPropiedad(reserva, usuarioId);

        reserva.setEstado("cancelada");
        return reservaRepository.save(reserva);
    }

    // ========================================================================
    // MÉTODOS PRIVADOS (REFACTORIZACIÓN PARA REDUCIR COMPLEJIDAD CICLOMÁTICA)
    // ========================================================================

    private User buscarUsuario(Integer usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ReservaException("El usuario no existe.", HttpStatus.NOT_FOUND));
    }

    private HorarioDisponible buscarHorario(Integer horarioId) {
        return horarioDisponibleRepository.findById(horarioId)
                .orElseThrow(() -> new ReservaException("El horario no existe.", HttpStatus.NOT_FOUND));
    }

    private void validarReglasDeCreacion(HorarioDisponible horario, LocalDate fecha, LocalTime hora) {
        if (!horario.getAgenda().isActiva()) {
            throw new ReservaException("Este horario no está habilitado por el proveedor.", HttpStatus.BAD_REQUEST);
        }

        if (LocalDateTime.of(fecha, hora).isBefore(LocalDateTime.now())) {
            throw new ReservaException("No se puede reservar en una fecha u hora que ya pasó.", HttpStatus.BAD_REQUEST);
        }

        if (hora.isBefore(horario.getHoraInicio()) || !hora.isBefore(horario.getHoraFin())) {
            throw new ReservaException("La hora seleccionada está fuera del rango de este horario.", HttpStatus.BAD_REQUEST);
        }

        if (reservaRepository.existsByHorario_IdAndFechaAndHoraAndEstadoIgnoreCase(horario.getId(), fecha, hora, ESTADO_ACTIVA)) {
            throw new ReservaException("El recurso no está disponible en la fecha y hora seleccionadas.", HttpStatus.CONFLICT);
        }
    }

    private void validarEstadoParaCancelar(Reserva reserva) {
        if (!ESTADO_ACTIVA.equalsIgnoreCase(reserva.getEstado())) {
            if ("cancelada".equalsIgnoreCase(reserva.getEstado())) {
                throw new ReservaException("La reserva ya está cancelada.", HttpStatus.BAD_REQUEST);
            }
            if ("completada".equalsIgnoreCase(reserva.getEstado())) {
                throw new ReservaException("La reserva ya fue completada y no puede cancelarse.", HttpStatus.BAD_REQUEST);
            }
            throw new ReservaException("La reserva no está activa.", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarPropiedad(Reserva reserva, Integer usuarioId) {
        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new ReservaException("No puedes cancelar una reserva que no te pertenece.", HttpStatus.FORBIDDEN);
        }
    }
}