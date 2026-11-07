package com.udea.reservas.plataformareservas.reservas.model;

import com.udea.reservas.plataformareservas.agendas.model.HorarioDisponible;
import com.udea.reservas.plataformareservas.recursos.model.Recurso;
import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import com.udea.reservas.plataformareservas.usuarios.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_id", nullable = false)
    private HorarioDisponible horario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurso_id")
    private Recurso recurso;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "activa";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}