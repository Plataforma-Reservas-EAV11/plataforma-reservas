package com.udea.reservas.plataformareservas.agendas.service;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;
import com.udea.reservas.plataformareservas.agendas.exception.AgendaException;
import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.agendas.repository.AgendaRepository;
import com.udea.reservas.plataformareservas.agendas.repository.HorarioDisponibleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HorarioDisponibleService {

    @Autowired
    private HorarioDisponibleRepository horarioRepository;

    @Autowired
    private AgendaRepository agendaRepository;

    public HorarioResponse crearHorario(Integer agendaId, CrearHorarioRequest request) {
        Agenda agenda = agendaRepository.findById(agendaId)
                .orElseThrow(() -> new AgendaException("La agenda con ID " + agendaId + " no existe."));

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