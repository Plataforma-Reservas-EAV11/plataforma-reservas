-- Ejecutar UNA VEZ en cada base de datos ya creada antes de desplegar esta versión
-- (requiere haber aplicado antes 2026-11-02-crear-servicio.sql).
-- No modifica ni elimina datos: las reservas existentes conservan servicio_id = NULL
-- y los servicios existentes quedan activos.
-- La aplicación valida el esquema al arrancar (spring.jpa.hibernate.ddl-auto=validate).
ALTER TABLE servicio
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT true;

ALTER TABLE reserva
    ADD COLUMN IF NOT EXISTS servicio_id INTEGER REFERENCES servicio(id);

CREATE INDEX IF NOT EXISTS ix_reserva_servicio_estado
    ON reserva (servicio_id, estado);
