package com.udea.reservas.plataformareservas;

import com.udea.reservas.plataformareservas.aceptacion.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "DB_PASSWORD=no-se-usa-con-testcontainers")
@Import(TestcontainersConfiguration.class)
class PlataformaReservasApplicationTests {

    @Test
    void contextLoads() {
    }

}
