package com.udea.reservas.plataformareservas.agendas.repository;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import java.time.LocalTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IHorarioDisponibleRepository
    extends JpaRepository<HorarioDisponible, Integer> {

    // Dos bloques se solapan si uno empieza antes de que termine el otro y termina
    // después de que el otro empiece; los bloques que solo se tocan no se solapan.
    boolean existsByAgenda_IdAndDiaSemanaAndHoraInicioLessThanAndHoraFinGreaterThan(
        Integer agendaId,
        Integer diaSemana,
        LocalTime horaFinNueva,
        LocalTime horaInicioNueva
    );
}
