package com.udea.reservas.plataformareservas.usuarios.repository;

import com.udea.reservas.plataformareservas.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface IUsuarioRepository extends JpaRepository<Usuario, Integer> {}
