package com.udea.reservas.plataformareservas.recursos.repository;

import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecursoRepository extends JpaRepository<Recurso, Integer> {

    List<Recurso> findAllByIdInAndProveedor_Id(
        Collection<Integer> recursoIds,
        Integer proveedorId
    );

    List<Recurso> findAllByProveedor_IdOrderByIdAsc(Integer proveedorId);

    Optional<Recurso> findByIdAndProveedor_Id(Integer id, Integer proveedorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select r
        from Recurso r
        where r.id = :recursoId
          and r.proveedor.id = :proveedorId
        """)
    Optional<Recurso> findOwnedForUpdate(
        @Param("recursoId") Integer recursoId,
        @Param("proveedorId") Integer proveedorId
    );

    @Query(
        value = """
            select r.*
            from recurso r
            join servicio_recurso sr on sr.recurso_id = r.id
            where sr.servicio_id = :servicioId
              and r.proveedor_id = :proveedorId
              and r.activo = true
            order by r.id
            for update of r
            """,
        nativeQuery = true
    )
    List<Recurso> findActiveCompatibleForUpdate(
        @Param("servicioId") Integer servicioId,
        @Param("proveedorId") Integer proveedorId
    );
}
