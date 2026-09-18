package com.udea.reservas.plataformareservas.agendas.service;

import com.udea.reservas.plataformareservas.agendas.dto.CrearHorarioRequest;
import com.udea.reservas.plataformareservas.agendas.dto.HorarioResponse;

public interface HorarioDisponibleService {

    HorarioResponse crearHorario(Integer agendaId, CrearHorarioRequest request);
}
