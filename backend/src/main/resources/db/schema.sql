CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE usuario (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL DEFAULT 'usuario',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta TIMESTAMP
);

CREATE TABLE proveedor (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    nombre_negocio VARCHAR(150) NOT NULL,
    telefono VARCHAR(30)
);

CREATE TABLE servicio (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    proveedor_id INTEGER NOT NULL REFERENCES proveedor(id),
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    duracion_minutos INTEGER NOT NULL CHECK (duracion_minutos > 0),
    eliminado BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE recurso (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    proveedor_id INTEGER NOT NULL REFERENCES proveedor(id),
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(50),
    activo BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE servicio_recurso (
    servicio_id INTEGER NOT NULL REFERENCES servicio(id),
    recurso_id INTEGER NOT NULL REFERENCES recurso(id),
    PRIMARY KEY (servicio_id, recurso_id)
);

CREATE TABLE agenda (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    proveedor_id INTEGER NOT NULL REFERENCES proveedor(id),
    recurso_id INTEGER REFERENCES recurso(id),
    servicio_id INTEGER REFERENCES servicio(id),
    nombre VARCHAR(150),
    activa BOOLEAN NOT NULL DEFAULT true
);

CREATE UNIQUE INDEX ux_agenda_servicio_unica
    ON agenda (servicio_id)
    WHERE servicio_id IS NOT NULL;

CREATE TABLE horario_disponible (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    agenda_id INTEGER NOT NULL REFERENCES agenda(id),
    dia_semana INTEGER NOT NULL CHECK (dia_semana BETWEEN 0 AND 6),
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    duracion_slot_min INTEGER NOT NULL CHECK (duracion_slot_min > 0),
    CHECK (hora_fin > hora_inicio)
);

CREATE TABLE reserva (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    horario_id INTEGER NOT NULL REFERENCES horario_disponible(id),
    servicio_id INTEGER REFERENCES servicio(id),
    recurso_id INTEGER REFERENCES recurso(id),
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    hora_fin TIME,
    estado VARCHAR(20) NOT NULL DEFAULT 'activa'
        CHECK (estado IN ('activa', 'cancelada', 'completada')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_reserva_activa_completa
        CHECK (
            estado <> 'activa'
            OR (servicio_id IS NOT NULL AND recurso_id IS NOT NULL AND hora_fin IS NOT NULL)
        ),
    CONSTRAINT chk_reserva_intervalo_valido
        CHECK (hora_fin IS NULL OR hora_fin > hora)
);

CREATE INDEX ix_reserva_recurso_fecha_activa
    ON reserva (recurso_id, fecha)
    WHERE estado = 'activa';

CREATE INDEX ix_reserva_servicio
    ON reserva (servicio_id);

ALTER TABLE reserva
    ADD CONSTRAINT ex_reserva_recurso_intervalo
    EXCLUDE USING gist (
        recurso_id WITH =,
        tsrange(fecha + hora, fecha + hora_fin, '[)') WITH &&
    )
    WHERE (estado = 'activa' AND recurso_id IS NOT NULL AND hora_fin IS NOT NULL);

CREATE FUNCTION validar_proveedor_servicio_recurso()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    proveedor_servicio INTEGER;
    proveedor_recurso INTEGER;
BEGIN
    SELECT proveedor_id INTO proveedor_servicio FROM servicio WHERE id = NEW.servicio_id;
    SELECT proveedor_id INTO proveedor_recurso FROM recurso WHERE id = NEW.recurso_id;
    IF proveedor_servicio IS DISTINCT FROM proveedor_recurso THEN
        RAISE EXCEPTION 'El servicio y el recurso deben pertenecer al mismo proveedor';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_proveedor_servicio_recurso
BEFORE INSERT OR UPDATE ON servicio_recurso
FOR EACH ROW EXECUTE FUNCTION validar_proveedor_servicio_recurso();

CREATE FUNCTION validar_proveedor_agenda_servicio()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    proveedor_servicio INTEGER;
    proveedor_recurso INTEGER;
BEGIN
    IF NEW.servicio_id IS NOT NULL THEN
        SELECT proveedor_id INTO proveedor_servicio FROM servicio WHERE id = NEW.servicio_id;
        IF proveedor_servicio IS DISTINCT FROM NEW.proveedor_id THEN
            RAISE EXCEPTION 'La agenda y el servicio deben pertenecer al mismo proveedor';
        END IF;
    END IF;
    IF NEW.recurso_id IS NOT NULL THEN
        SELECT proveedor_id INTO proveedor_recurso FROM recurso WHERE id = NEW.recurso_id;
        IF proveedor_recurso IS DISTINCT FROM NEW.proveedor_id THEN
            RAISE EXCEPTION 'La agenda y el recurso deben pertenecer al mismo proveedor';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_proveedor_agenda_servicio
BEFORE INSERT OR UPDATE ON agenda
FOR EACH ROW EXECUTE FUNCTION validar_proveedor_agenda_servicio();

CREATE FUNCTION validar_reserva_servicio_recurso()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    proveedor_servicio INTEGER;
    proveedor_recurso INTEGER;
    servicio_agenda INTEGER;
BEGIN
    IF NEW.servicio_id IS NULL OR NEW.recurso_id IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT proveedor_id INTO proveedor_servicio FROM servicio WHERE id = NEW.servicio_id;
    SELECT proveedor_id INTO proveedor_recurso FROM recurso WHERE id = NEW.recurso_id;
    SELECT a.servicio_id
      INTO servicio_agenda
      FROM horario_disponible h
      JOIN agenda a ON a.id = h.agenda_id
     WHERE h.id = NEW.horario_id;
    IF proveedor_servicio IS DISTINCT FROM proveedor_recurso
       OR servicio_agenda IS DISTINCT FROM NEW.servicio_id
       OR NOT EXISTS (
           SELECT 1
             FROM servicio_recurso sr
            WHERE sr.servicio_id = NEW.servicio_id
              AND sr.recurso_id = NEW.recurso_id
       ) THEN
        RAISE EXCEPTION 'La reserva requiere un recurso compatible del proveedor y agenda del servicio';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_reserva_servicio_recurso
BEFORE INSERT OR UPDATE ON reserva
FOR EACH ROW EXECUTE FUNCTION validar_reserva_servicio_recurso();
