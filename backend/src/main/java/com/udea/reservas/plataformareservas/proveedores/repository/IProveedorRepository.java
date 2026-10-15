package com.udea.reservas.plataformareservas.proveedores.repository;

import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IProveedorRepository extends JpaRepository<Proveedor, Integer> {}
