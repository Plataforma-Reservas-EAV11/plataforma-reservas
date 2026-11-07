package com.udea.reservas.plataformareservas.reservas.repository;

import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IReservaRepository extends JpaRepository<Reserva, Integer> {

    boolean existsByHorario_IdAndFechaAndHoraAndEstadoIgnoreCase(
            Integer horarioId,
            LocalDate fecha,
            LocalTime hora,
            String estado
    );

    List<Reserva> findAllByUsuario_IdAndEstadoIgnoreCaseOrderByFechaAscHoraAsc(
            Integer usuarioId,
            String estado
    );

    @EntityGraph(attributePaths = "horario")
    List<Reserva> findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
            Integer proveedorId,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String estado
    );
}