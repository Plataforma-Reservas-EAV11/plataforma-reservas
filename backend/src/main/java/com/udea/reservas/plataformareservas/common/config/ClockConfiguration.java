package com.udea.reservas.plataformareservas.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock(@Value("${app.zona-horaria:America/Bogota}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
