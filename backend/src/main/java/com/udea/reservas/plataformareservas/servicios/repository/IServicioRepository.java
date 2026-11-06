package com.udea.reservas.plataformareservas.servicios.repository;

import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IServicioRepository extends JpaRepository<Servicio, Integer> {

    @EntityGraph(attributePaths = "proveedor")
    List<Servicio> findAllByProveedor_IdOrderByNombreAscIdAsc(Integer proveedorId);
}
