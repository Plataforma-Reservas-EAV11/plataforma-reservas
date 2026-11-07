package com.udea.reservas.plataformareservas.proveedores.repository;

import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IProveedorRepository extends JpaRepository<Proveedor, Integer> {

    List<Proveedor> findAllByUsuario_Id(Integer usuarioId);

    Page<Proveedor> findAllByOrderByNombreNegocioAscIdAsc(Pageable pageable);
}
