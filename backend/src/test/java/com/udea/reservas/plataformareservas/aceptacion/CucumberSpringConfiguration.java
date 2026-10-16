package com.udea.reservas.plataformareservas.aceptacion;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@CucumberContextConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "DB_PASSWORD=no-se-usa-con-testcontainers",
        "SESSION_COOKIE_SECURE=false"
    }
)
@Import(TestcontainersConfiguration.class)
class CucumberSpringConfiguration {}
