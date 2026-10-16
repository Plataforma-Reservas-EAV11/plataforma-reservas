package com.udea.reservas.plataformareservas.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClockConfigurationTest {

    @Test
    @DisplayName("Crea un reloj del sistema en la zona horaria configurada")
    void clockUsaLaZonaHorariaConfigurada() {
        // Arrange
        ClockConfiguration configuration = new ClockConfiguration();

        // Act
        Clock clock = configuration.clock("America/Bogota");

        // Assert
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("America/Bogota"));
    }
}
