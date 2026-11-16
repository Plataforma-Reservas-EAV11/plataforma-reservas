package com.udea.reservas.plataformareservas.servicios.repository;

import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IServicioRepository extends JpaRepository<Servicio, Integer> {

    @EntityGraph(attributePaths = "proveedor")
    @Query("select s from Servicio s where s.proveedor.id = :proveedorId and s.eliminado = false order by s.nombre, s.id")
    List<Servicio> findAllByProveedor_IdOrderByNombreAscIdAsc(@Param("proveedorId") Integer proveedorId);

    @Query("select s from Servicio s where s.id = :id and s.proveedor.id = :proveedorId and s.eliminado = false")
    Optional<Servicio> findByIdAndProveedor_Id(@Param("id") Integer id, @Param("proveedorId") Integer proveedorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Servicio s where s.id = :id and s.eliminado = false")
    Optional<Servicio> findActivoForUpdate(@Param("id") Integer id);
}
