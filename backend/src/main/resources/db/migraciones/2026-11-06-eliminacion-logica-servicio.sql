-- Ejecutar antes del despliegue en bases que ya tengan la tabla servicio.
ALTER TABLE servicio ADD COLUMN IF NOT EXISTS eliminado BOOLEAN NOT NULL DEFAULT false;
