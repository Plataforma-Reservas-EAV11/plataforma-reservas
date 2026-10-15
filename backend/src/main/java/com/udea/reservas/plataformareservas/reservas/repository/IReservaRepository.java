package com.udea.reservas.plataformareservas.reservas.repository;

import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDate;
import java.time.LocalTime;

@NoRepositoryBean
public interface IReservaRepository extends JpaRepository<Reserva, Integer> {

    boolean existsByHorario_IdAndFechaAndHoraAndEstadoIgnoreCase(
            Integer horarioId,
            LocalDate fecha,
            LocalTime hora,
            String estado
    );
}