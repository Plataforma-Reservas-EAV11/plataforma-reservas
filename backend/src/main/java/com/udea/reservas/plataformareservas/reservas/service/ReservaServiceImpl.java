package com.udea.reservas.plataformareservas.reservas.service;

import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import com.udea.reservas.plataformareservas.reservas.dto.ActiveReservationResponse;
import com.udea.reservas.plataformareservas.reservas.exception.ReservaException;
import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import com.udea.reservas.plataformareservas.reservas.repository.IReservaRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
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

    private final IAgendaRepository agendaRepository;

    private final IServicioRepository servicioRepository;

    private final RecursoRepository recursoRepository;

    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<ActiveReservationResponse> getActiveReservations(Integer usuarioId) {
        return reservaRepository
            .findAllByUsuario_IdAndEstadoIgnoreCaseOrderByFechaAscHoraAsc(
                usuarioId,
                ESTADO_ACTIVA
            )
            .stream()
            .filter(Objects::nonNull)
            .filter(reserva ->
                reserva.getEstado() != null &&
                ESTADO_ACTIVA.equalsIgnoreCase(reserva.getEstado())
            )
            .map(reserva ->
                new ActiveReservationResponse(
                    reserva.getId(),
                    reserva.getServicio() == null ? null : reserva.getServicio().getNombre(),
                    reserva.getFecha(),
                    reserva.getHora(),
                    reserva.getHoraFin(),
                    reserva.getEstado()
                )
            )
            .toList();
    }

    @Override
    @Transactional
    public Reserva crearReserva(
        Integer usuarioId,
        Integer servicioId,
        LocalDate fecha,
        LocalTime hora
    ) {
        User usuario = usuarioRepository
            .findById(usuarioId)
            .orElseThrow(noExiste("El usuario no existe."));

        Servicio servicio = servicioRepository.findActivoForUpdate(servicioId)
            .orElseThrow(noExiste("El servicio no existe."));

        Agenda agenda = agendaRepository.findByServicio_Id(servicioId)
            .orElseThrow(noExiste("El servicio no tiene una agenda configurada."));

        // Regla: el horario debe estar habilitado por el proveedor (agenda activa)
        rechazarSi(
            !agenda.isActiva(),
            "Este horario no está habilitado por el proveedor.",
            HttpStatus.BAD_REQUEST
        );
        rechazarSi(
            agenda.getProveedor() == null
                || !agenda.getProveedor().getId().equals(servicio.getProveedor().getId()),
            "La agenda no pertenece al proveedor del servicio.",
            HttpStatus.BAD_REQUEST
        );

        // Regla: no se puede reservar en una fecha/hora que ya pasó
        rechazarSi(
            LocalDateTime.of(fecha, hora).isBefore(LocalDateTime.now(clock)),
            "No se puede reservar en una fecha u hora que ya pasó.",
            HttpStatus.BAD_REQUEST
        );

        Integer duracion = servicio.getDuracionMinutos();
        if (duracion == null || duracion <= 0) {
            throw new ReservaException(
                "La duración del servicio no es válida.",
                HttpStatus.BAD_REQUEST
            );
        }
        LocalTime horaFin = hora.plusMinutes(duracion);
        rechazarSi(
            !horaFin.isAfter(hora),
            "La duración del servicio excede el horario disponible.",
            HttpStatus.BAD_REQUEST
        );

        // Regla: la fecha debe caer en el día de la semana del horario (0 = lunes ... 6 = domingo)
        int diaSemanaFecha = fecha.getDayOfWeek().getValue() - 1;
        List<HorarioDisponible> horariosDelDia =
            horarioDisponibleRepository.findAllByAgenda_IdAndDiaSemanaOrderByHoraInicioAsc(
                agenda.getId(),
                diaSemanaFecha
            );
        if (horariosDelDia.isEmpty()) {
            throw new ReservaException(
                "La fecha seleccionada no corresponde al día habilitado para este horario.",
                HttpStatus.BAD_REQUEST
            );
        }

        Optional<HorarioDisponible> bloqueQueContieneInicio = horariosDelDia.stream()
            .filter(bloque -> !hora.isBefore(bloque.getHoraInicio()))
            .filter(bloque -> hora.isBefore(bloque.getHoraFin()))
            .findFirst();
        if (bloqueQueContieneInicio.isEmpty()) {
            throw new ReservaException(
                "La hora seleccionada está fuera del rango de este horario.",
                HttpStatus.BAD_REQUEST
            );
        }
        HorarioDisponible horario = bloqueQueContieneInicio.get();
        rechazarSi(
            horaFin.isAfter(horario.getHoraFin()),
            "La duración del servicio excede el horario disponible.",
            HttpStatus.BAD_REQUEST
        );

        // Lock all eligible resources in a stable order so concurrent bookings
        // serialize before checking their occupied intervals.
        List<Recurso> recursos = recursoRepository.findActiveCompatibleForUpdate(
            servicioId,
            servicio.getProveedor().getId()
        );
        if (recursos.isEmpty()) {
            throw new ReservaException(
                "El servicio aún no tiene recursos disponibles para reservar.",
                HttpStatus.CONFLICT
            );
        }

        Recurso recursoDisponible = recursos.stream()
            .filter(recurso ->
                !reservaRepository.existsReservaActivaSolapada(
                    recurso.getId(),
                    fecha,
                    hora,
                    horaFin
                )
            )
            .findFirst()
            .orElseThrow(() ->
                new ReservaException(MENSAJE_NO_DISPONIBLE, HttpStatus.CONFLICT)
            );

        Reserva reserva = Reserva.builder()
            .usuario(usuario)
            .horario(horario)
            .servicio(servicio)
            .recurso(recursoDisponible)
            .fecha(fecha)
            .hora(hora)
            .horaFin(horaFin)
            .estado(ESTADO_ACTIVA)
            .createdAt(LocalDateTime.now(clock))
            .build();

        try {
            return reservaRepository.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException exception) {
            if (!esConflictoDeIntervalo(exception)) {
                throw exception;
            }
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

    private static boolean esConflictoDeIntervalo(Throwable exception) {
        Throwable actual = exception;
        while (actual != null) {
            if (
                actual instanceof ConstraintViolationException violation
                    && "ex_reserva_recurso_intervalo".equalsIgnoreCase(
                        violation.getConstraintName()
                    )
            ) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }
}
