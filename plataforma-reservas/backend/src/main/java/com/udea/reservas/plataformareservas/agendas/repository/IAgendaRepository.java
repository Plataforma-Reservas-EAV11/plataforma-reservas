package com.udea.reservas.plataformareservas.agendas.repository;

import com.udea.reservas.plataformareservas.agendas.model.Agenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IAgendaRepository extends JpaRepository<Agenda, Integer> {}
