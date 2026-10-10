package com.udea.reservas.plataformareservas.agendas.repository;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IHorarioDisponibleRepository
    extends JpaRepository<HorarioDisponible, Integer> {}
