-- DEF-13: bloqueo temporal de la cuenta tras intentos fallidos de inicio de sesión.
--
-- Ejecutar UNA VEZ en cada base de datos ya creada (Supabase, desarrollo local...)
-- ANTES de desplegar esta versión: la aplicación valida el esquema al arrancar
-- (spring.jpa.hibernate.ddl-auto=validate) y falla si faltan estas columnas.
-- Las bases nuevas ya las traen en db/schema.sql.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS intentos_fallidos INTEGER NOT NULL DEFAULT 0;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS bloqueado_hasta TIMESTAMP;
