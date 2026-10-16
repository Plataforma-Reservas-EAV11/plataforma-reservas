package com.udea.reservas.plataformareservas.aceptacion;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    // Se usa el mismo script que define la base de datos real, de modo que las
    // restricciones (CHECK, índice único parcial) se prueban tal como existen.
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withInitScript("db/schema.sql");
    }
}
