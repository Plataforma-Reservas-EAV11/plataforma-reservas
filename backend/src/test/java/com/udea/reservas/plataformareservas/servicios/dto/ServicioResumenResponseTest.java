package com.udea.reservas.plataformareservas.servicios.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.udea.reservas.plataformareservas.servicios.model.Servicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServicioResumenResponseTest {

    @Test
    @DisplayName("Expone solo id, nombre, duración y estado activo")
    void fromEntityMapeaLosCamposDelResumen() {
        Servicio servicio = Servicio.builder()
            .id(41)
            .nombre("Corte")
            .descripcion("Descripción interna")
            .duracionMinutos(30)
            .build();

        ServicioResumenResponse response = ServicioResumenResponse.fromEntity(servicio);

        assertThat(response).isEqualTo(new ServicioResumenResponse(41, "Corte", 30, "activo"));
        assertThat(response.estado()).isEqualTo(ServicioResumenResponse.ESTADO_ACTIVO);
    }
}
