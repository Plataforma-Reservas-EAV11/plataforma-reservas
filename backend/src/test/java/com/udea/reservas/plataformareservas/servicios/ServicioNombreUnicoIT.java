package com.udea.reservas.plataformareservas.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.udea.reservas.plataformareservas.aceptacion.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;

@SpringBootTest(properties = "DB_PASSWORD=no-se-usa-con-testcontainers")
@Import(TestcontainersConfiguration.class)
class ServicioNombreUnicoIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void migracionDetieneDuplicadosSinModificarDatos() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement sql = connection.createStatement()) {
            String schema = crearEsquema(connection, sql);
            try {
                sql.execute("insert into servicio values (1, 'Corte', false), (1, ' corte ', false)");
                assertThatThrownBy(() -> sql.execute(migracion()))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("Existen servicios activos con nombres duplicados");
                sql.execute("ROLLBACK");
                try (var rows = sql.executeQuery("select count(*) from servicio")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(2);
                }
            } finally {
                eliminarEsquema(connection, sql, schema);
            }
        }
    }

    @Test
    void migracionProtegeSoloNombresActivosDelMismoProveedor() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement sql = connection.createStatement()) {
            String schema = crearEsquema(connection, sql);
            try {
                sql.execute("insert into servicio values (1, 'Corte', false), (2, 'corte', false), (1, ' corte ', true)");
                sql.execute(migracion());
                assertThatThrownBy(() -> sql.execute("insert into servicio values (1, ' CORTE ', false)"))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("ux_servicio_proveedor_nombre_activo");
                sql.execute("insert into servicio values (1, 'Manicure', false)");
                try (var rows = sql.executeQuery("select count(*) from servicio")) {
                    rows.next();
                    assertThat(rows.getInt(1)).isEqualTo(4);
                }
            } finally {
                eliminarEsquema(connection, sql, schema);
            }
        }
    }

    private static String crearEsquema(Connection connection, Statement sql) throws SQLException {
        String schema = "hu10_" + UUID.randomUUID().toString().replace("-", "");
        sql.execute("create schema " + schema);
        connection.setSchema(schema);
        sql.execute("create table servicio (proveedor_id integer not null, nombre varchar(150) not null, eliminado boolean not null)");
        return schema;
    }

    private static void eliminarEsquema(Connection connection, Statement sql, String schema) throws SQLException {
        connection.setSchema("public");
        sql.execute("drop schema " + schema + " cascade");
    }

    private static String migracion() throws Exception {
        return new ClassPathResource("db/migraciones/2026-11-18-nombre-servicio-unico.sql")
            .getContentAsString(StandardCharsets.UTF_8);
    }
}
