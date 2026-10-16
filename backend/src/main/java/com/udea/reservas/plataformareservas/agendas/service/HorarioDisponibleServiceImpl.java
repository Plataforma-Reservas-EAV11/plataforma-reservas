package com.udea.reservas.plataformareservas.agendas.service;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.exception.AgendaException;
import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.IAgendaRepository;
import com.udea.reservas.plataformareservas.agendas.repository.IHorarioDisponibleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HorarioDisponibleServiceImpl implements HorarioDisponibleService {

    private final IHorarioDisponibleRepository horarioRepository;

    private final IAgendaRepository agendaRepository;

    @Override
    @Transactional
    public HorarioResponse crearHorario(
        Integer agendaId,
        CrearHorarioRequest request
    ) {
        Agenda agenda = agendaRepository
            .findById(agendaId)
            .orElseThrow(() ->
                new AgendaException(
                    "La agenda con ID " + agendaId + " no existe.",
                    HttpStatus.NOT_FOUND
                )
            );

        // Regla: la hora de fin debe ser posterior a la hora de inicio
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new AgendaException(
                "El rango de horas no es válido. La hora de fin debe ser posterior a la de inicio.",
                HttpStatus.BAD_REQUEST
            );
        }

        // Regla: dos bloques de la misma agenda y día no pueden solaparse
        boolean seSolapa =
            horarioRepository.existsByAgenda_IdAndDiaSemanaAndHoraInicioLessThanAndHoraFinGreaterThan(
                agendaId,
                request.diaSemana(),
                request.horaFin(),
                request.horaInicio()
            );
        if (seSolapa) {
            throw new AgendaException(
                "El horario ya tiene disponibilidad definida. No se pueden solapar bloques.",
                HttpStatus.BAD_REQUEST
            );
        }

        HorarioDisponible horario = new HorarioDisponible();
        horario.setAgenda(agenda);
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setDuracionSlotMin(request.duracionSlotMin());

        HorarioDisponible guardado = horarioRepository.save(horario);

        return new HorarioResponse(
            guardado.getId(),
            agenda.getId(),
            guardado.getDiaSemana(),
            guardado.getHoraInicio(),
            guardado.getHoraFin(),
            guardado.getDuracionSlotMin()
        );
    }
}
