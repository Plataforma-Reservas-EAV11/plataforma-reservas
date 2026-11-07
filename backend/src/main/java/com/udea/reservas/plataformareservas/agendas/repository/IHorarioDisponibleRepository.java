package com.udea.reservas.plataformareservas.agendas.repository;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import java.util.List;
import java.time.LocalTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IHorarioDisponibleRepository
    extends JpaRepository<HorarioDisponible, Integer> {

    List<HorarioDisponible> findAllByAgenda_IdAndDiaSemanaOrderByHoraInicioAsc(
        Integer agendaId,
        Integer diaSemana
    );

    // Dos bloques se solapan si uno empieza antes de que termine el otro y termina
    // después de que el otro empiece; los bloques que solo se tocan no se solapan.
    boolean existsByAgenda_IdAndDiaSemanaAndHoraInicioLessThanAndHoraFinGreaterThan(
        Integer agendaId,
        Integer diaSemana,
        LocalTime horaFinNueva,
        LocalTime horaInicioNueva
    );

    @EntityGraph(attributePaths = "agenda")
    List<HorarioDisponible> findAllByAgenda_Proveedor_IdAndAgenda_ActivaTrueOrderByIdAsc(
        Integer proveedorId
    );
}
