package com.udea.reservas.plataformareservas.reservas.repository;

import com.udea.reservas.plataformareservas.reservas.model.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

@NoRepositoryBean
public interface IReservaRepository extends JpaRepository<Reserva, Integer> {

    @Query("""
        select count(r) > 0
        from Reserva r
        where r.recurso.id = :recursoId
          and r.fecha = :fecha
          and r.estado = 'activa'
          and r.hora < :horaFin
          and r.horaFin > :horaInicio
        """)
    boolean existsReservaActivaSolapada(
        @Param("recursoId") Integer recursoId,
        @Param("fecha") LocalDate fecha,
        @Param("horaInicio") LocalTime horaInicio,
        @Param("horaFin") LocalTime horaFin
    );

    boolean existsByHorario_IdAndFechaAndHoraAndEstadoIgnoreCase(
        Integer horarioId,
        LocalDate fecha,
        LocalTime hora,
        String estado
    );

    @EntityGraph(attributePaths = {"horario", "servicio"})
    List<Reserva> findAllByUsuario_IdAndEstadoIgnoreCaseOrderByFechaAscHoraAsc(
        Integer usuarioId,
        String estado
    );

    @EntityGraph(attributePaths = {"horario", "recurso"})
    List<Reserva> findAllByHorario_Agenda_Proveedor_IdAndFechaBetweenAndEstadoIgnoreCase(
        Integer proveedorId,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado
    );
}