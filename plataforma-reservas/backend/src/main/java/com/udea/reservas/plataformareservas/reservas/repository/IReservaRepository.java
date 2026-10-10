package com.udea.reservas.plataformareservas.reservas.repository;

import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IReservaRepository extends JpaRepository<Reserva, Integer> {
    boolean existsByHorario_IdAndFechaAndEstado(
        Integer horarioId,
        LocalDate fecha,
        String estado
    );
}
