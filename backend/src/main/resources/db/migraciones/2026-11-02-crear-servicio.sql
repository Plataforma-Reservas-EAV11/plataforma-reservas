-- Ejecutar UNA VEZ en cada base de datos ya creada antes de desplegar esta versión.
-- No modifica ni elimina perfiles de proveedor existentes ni impone unicidad de nombre.
-- La aplicación valida el esquema al arrancar (spring.jpa.hibernate.ddl-auto=validate).
CREATE TABLE IF NOT EXISTS servicio (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    proveedor_id INTEGER NOT NULL REFERENCES proveedor(id),
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    duracion_minutos INTEGER NOT NULL CHECK (duracion_minutos > 0)
);
