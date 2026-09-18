package com.udea.reservas.plataformareservas.reservas.service;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.HorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.repository.ReservaRepository;
import com.udea.reservas.plataformareservas.usuarios.model.Usuario;
import com.udea.reservas.plataformareservas.usuarios.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    @Autowired
    private final ReservaRepository reservaRepository;

    @Autowired
    private final UsuarioRepository usuarioRepository;

    @Autowired
    private final HorarioDisponibleRepository horarioDisponibleRepository;

    @Override
    @Transactional
    public Reserva crearReserva(
        Integer usuarioId,
        Integer horarioId,
        LocalDate fecha,
        LocalTime hora
    ) {
        Usuario usuario = usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() ->
                new ReservaException(
                    "El usuario no existe.",
                    HttpStatus.NOT_FOUND
                )
            );

        HorarioDisponible horario = horarioDisponibleRepository
            .findById(horarioId)
            .orElseThrow(() ->
                new ReservaException(
                    "El horario no existe.",
                    HttpStatus.NOT_FOUND
                )
            );

        // Regla: el horario debe estar habilitado por el proveedor (agenda activa)
        if (!horario.getAgenda().isActiva()) {
            throw new ReservaException(
                "Este horario no está habilitado por el proveedor.",
                HttpStatus.BAD_REQUEST
            );
        }

        // Regla: no se puede reservar en una fecha/hora que ya pasó
        LocalDateTime fechaHoraReserva = LocalDateTime.of(fecha, hora);
        if (fechaHoraReserva.isBefore(LocalDateTime.now())) {
            throw new ReservaException(
                "No se puede reservar en una fecha u hora que ya pasó.",
                HttpStatus.BAD_REQUEST
            );
        }

        // Regla: la hora debe estar dentro del rango del horario disponible
        if (
            hora.isBefore(horario.getHoraInicio()) ||
            !hora.isBefore(horario.getHoraFin())
        ) {
            throw new ReservaException(
                "La hora seleccionada está fuera del rango de este horario.",
                HttpStatus.BAD_REQUEST
            );
        }

        // Regla: evitar doble reserva sobre el mismo horario y fecha
        boolean yaOcupado =
            reservaRepository.existsByHorario_IdAndFechaAndEstado(
                horarioId,
                fecha,
                "activa"
            );
        if (yaOcupado) {
            throw new ReservaException(
                "Este horario ya está reservado para esa fecha.",
                HttpStatus.CONFLICT
            );
        }

        Reserva reserva = Reserva.builder()
            .usuario(usuario)
            .horario(horario)
            .fecha(fecha)
            .hora(hora)
            .estado("activa")
            .build();

        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional
    public Reserva cancelarReserva(Integer reservaId, Integer usuarioId) {
        Reserva reserva = reservaRepository
            .findById(reservaId)
            .orElseThrow(() ->
                new ReservaException(
                    "La reserva no existe.",
                    HttpStatus.NOT_FOUND
                )
            );

        if (!"activa".equalsIgnoreCase(reserva.getEstado())) {
            if ("cancelada".equalsIgnoreCase(reserva.getEstado())) {
                throw new ReservaException(
                    "La reserva ya está cancelada.",
                    HttpStatus.BAD_REQUEST
                );
            }
            if ("completada".equalsIgnoreCase(reserva.getEstado())) {
                throw new ReservaException(
                    "La reserva ya fue completada y no puede cancelarse.",
                    HttpStatus.BAD_REQUEST
                );
            }
            throw new ReservaException(
                "La reserva no está activa.",
                HttpStatus.BAD_REQUEST
            );
        }

        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new ReservaException(
                "No puedes cancelar una reserva que no te pertenece.",
                HttpStatus.FORBIDDEN
            );
        }

        reserva.setEstado("cancelada");
        return reservaRepository.save(reserva);
    }
}
