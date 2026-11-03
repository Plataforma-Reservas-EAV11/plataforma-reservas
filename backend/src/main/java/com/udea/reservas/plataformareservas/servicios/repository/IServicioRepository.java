package com.udea.reservas.plataformareservas.servicios.repository;

import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IServicioRepository extends JpaRepository<Servicio, Integer> {}
