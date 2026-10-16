package com.udea.reservas.plataformareservas.reservas.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private static final String ESTADO_ACTIVA = "activa";

    private static final String MENSAJE_NO_DISPONIBLE =
        "El recurso no está disponible en la fecha y hora seleccionadas.";

    private final IReservaRepository reservaRepository;

    private final UserRepository usuarioRepository;

    private final IHorarioDisponibleRepository horarioDisponibleRepository;

    private final Clock clock;

    @Override
    @Transactional
    public Reserva crearReserva(
        Integer usuarioId,
        Integer horarioId,
        LocalDate fecha,
        LocalTime hora
    ) {
        User usuario = usuarioRepository
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
        if (fechaHoraReserva.isBefore(LocalDateTime.now(clock))) {
            throw new ReservaException(
                "No se puede reservar en una fecha u hora que ya pasó.",
                HttpStatus.BAD_REQUEST
            );
        }

        // Regla: la fecha debe caer en el día de la semana del horario (0 = domingo ... 6 = sábado)
        int diaSemanaFecha = fecha.getDayOfWeek().getValue() % 7;
        if (!horario.getDiaSemana().equals(diaSemanaFecha)) {
            throw new ReservaException(
                "La fecha seleccionada no corresponde al día habilitado para este horario.",
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
            reservaRepository.existsByHorario_IdAndFechaAndHoraAndEstadoIgnoreCase(
                horarioId,
                fecha,
                hora,
                ESTADO_ACTIVA
            );
        if (yaOcupado) {
            throw new ReservaException(MENSAJE_NO_DISPONIBLE, HttpStatus.CONFLICT);
        }

        Reserva reserva = Reserva.builder()
            .usuario(usuario)
            .horario(horario)
            .fecha(fecha)
            .hora(hora)
            .estado(ESTADO_ACTIVA)
            .build();

        // El índice único de la base de datos resuelve las solicitudes simultáneas
        // que pasaron la validación anterior al mismo tiempo.
        try {
            return reservaRepository.save(reserva);
        } catch (DataIntegrityViolationException ex) {
            throw new ReservaException(MENSAJE_NO_DISPONIBLE, HttpStatus.CONFLICT);
        }
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

        // La propiedad se valida antes que el estado para no revelar
        // información de reservas ajenas.
        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new ReservaException(
                "No puedes cancelar una reserva que no te pertenece.",
                HttpStatus.FORBIDDEN
            );
        }

        if (!ESTADO_ACTIVA.equalsIgnoreCase(reserva.getEstado())) {
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

        reserva.setEstado("cancelada");
        return reservaRepository.save(reserva);
    }
}
