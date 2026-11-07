package com.udea.reservas.plataformareservas.agendas.model;

import com.udea.reservas.plataformareservas.proveedores.model.Proveedor;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agenda")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @Column(length = 150)
    private String nombre;

    @Column(nullable = false)
    private boolean activa;
}