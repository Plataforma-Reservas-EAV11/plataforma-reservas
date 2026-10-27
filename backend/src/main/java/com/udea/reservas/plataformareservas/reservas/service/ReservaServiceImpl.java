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
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
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

    private static final String MENSAJE_NO_ACTIVA = "La reserva no está activa.";

    // Mensaje de cancelación según el estado actual; cualquier otro estado usa MENSAJE_NO_ACTIVA.
    private static final Map<String, String> MENSAJES_NO_CANCELABLE = Map.of(
        "cancelada",
        "La reserva ya está cancelada.",
        "completada",
        "La reserva ya fue completada y no puede cancelarse."
    );

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
            .orElseThrow(noExiste("El usuario no existe."));

        HorarioDisponible horario = horarioDisponibleRepository
            .findById(horarioId)
            .orElseThrow(noExiste("El horario no existe."));

        // Regla: el horario debe estar habilitado por el proveedor (agenda activa)
        rechazarSi(
            !horario.getAgenda().isActiva(),
            "Este horario no está habilitado por el proveedor.",
            HttpStatus.BAD_REQUEST
        );

        // Regla: no se puede reservar en una fecha/hora que ya pasó
        rechazarSi(
            LocalDateTime.of(fecha, hora).isBefore(LocalDateTime.now(clock)),
            "No se puede reservar en una fecha u hora que ya pasó.",
            HttpStatus.BAD_REQUEST
        );

        // Regla: la fecha debe caer en el día de la semana del horario (0 = lunes ... 6 = domingo)
        int diaSemanaFecha = fecha.getDayOfWeek().getValue() - 1;
        rechazarSi(
            !horario.getDiaSemana().equals(diaSemanaFecha),
            "La fecha seleccionada no corresponde al día habilitado para este horario.",
            HttpStatus.BAD_REQUEST
        );

        // Regla: la hora debe estar dentro del rango del horario disponible
        rechazarSi(
            hora.isBefore(horario.getHoraInicio()) ||
            !hora.isBefore(horario.getHoraFin()),
            "La hora seleccionada está fuera del rango de este horario.",
            HttpStatus.BAD_REQUEST
        );

        // Regla: evitar doble reserva sobre el mismo horario y fecha
        rechazarSi(
            reservaRepository.existsByHorario_IdAndFechaAndHoraAndEstadoIgnoreCase(
                horarioId,
                fecha,
                hora,
                ESTADO_ACTIVA
            ),
            MENSAJE_NO_DISPONIBLE,
            HttpStatus.CONFLICT
        );

        Reserva reserva = Reserva.builder()
            .usuario(usuario)
            .horario(horario)
            .fecha(fecha)
            .hora(hora)
            .estado(ESTADO_ACTIVA)
            .createdAt(LocalDateTime.now(clock))
            .build();

        // El índice único de la base de datos resuelve las solicitudes simultáneas
        // que pasaron la validación anterior al mismo tiempo.
        try {
            return reservaRepository.save(reserva);
        } catch (DataIntegrityViolationException _) {
            throw new ReservaException(MENSAJE_NO_DISPONIBLE, HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public Reserva cancelarReserva(Integer reservaId, Integer usuarioId) {
        Reserva reserva = reservaRepository
            .findById(reservaId)
            .orElseThrow(noExiste("La reserva no existe."));

        // La propiedad se valida antes que el estado para no revelar
        // información de reservas ajenas.
        rechazarSi(
            !reserva.getUsuario().getId().equals(usuarioId),
            "No puedes cancelar una reserva que no te pertenece.",
            HttpStatus.FORBIDDEN
        );

        if (!ESTADO_ACTIVA.equalsIgnoreCase(reserva.getEstado())) {
            // String.valueOf tolera un estado nulo (cae en el mensaje genérico).
            String estado = String.valueOf(reserva.getEstado()).toLowerCase(Locale.ROOT);
            throw new ReservaException(
                MENSAJES_NO_CANCELABLE.getOrDefault(estado, MENSAJE_NO_ACTIVA),
                HttpStatus.BAD_REQUEST
            );
        }

        reserva.setEstado("cancelada");
        return reservaRepository.save(reserva);
    }

    private static void rechazarSi(boolean condicion, String mensaje, HttpStatus status) {
        if (condicion) {
            throw new ReservaException(mensaje, status);
        }
    }

    private static Supplier<ReservaException> noExiste(String mensaje) {
        return () -> new ReservaException(mensaje, HttpStatus.NOT_FOUND);
    }
}
