-- Ejecutar una vez tras la migracion de eliminacion logica, antes del despliegue.
-- No modifica los datos existentes. Requiere resolver duplicados antes de reintentar.
BEGIN;

LOCK TABLE servicio IN SHARE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM servicio
        WHERE eliminado = false
        GROUP BY proveedor_id, lower(btrim(nombre))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Existen servicios activos con nombres duplicados por proveedor; resolverlos antes de aplicar la migracion.';
    END IF;
END;
$$;

CREATE UNIQUE INDEX ux_servicio_proveedor_nombre_activo
    ON servicio (proveedor_id, lower(btrim(nombre)))
    WHERE eliminado = false;

COMMIT;
