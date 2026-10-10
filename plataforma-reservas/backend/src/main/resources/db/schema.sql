-- usuario
CREATE TABLE usuario (
                         id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         nombre VARCHAR(150) NOT NULL,
                         email VARCHAR(150) NOT NULL UNIQUE,
                         password_hash VARCHAR(255) NOT NULL,
                         rol VARCHAR(30) NOT NULL DEFAULT 'usuario',
                         created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- proveedor
CREATE TABLE proveedor (
                           id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           usuario_id INTEGER NOT NULL REFERENCES usuario(id),
                           nombre_negocio VARCHAR(150) NOT NULL,
                           telefono VARCHAR(30)
);

-- recurso
CREATE TABLE recurso (
                         id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         proveedor_id INTEGER NOT NULL REFERENCES proveedor(id),
                         nombre VARCHAR(150) NOT NULL,
                         tipo VARCHAR(50),
                         activo BOOLEAN NOT NULL DEFAULT true
);

-- agenda
CREATE TABLE agenda (
                        id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        proveedor_id INTEGER NOT NULL REFERENCES proveedor(id),
                        recurso_id INTEGER NOT NULL REFERENCES recurso(id),
                        nombre VARCHAR(150),
                        activa BOOLEAN NOT NULL DEFAULT true
);

-- horario_disponible
CREATE TABLE horario_disponible (
                                    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                    agenda_id INTEGER NOT NULL REFERENCES agenda(id),
                                    dia_semana INTEGER NOT NULL CHECK (dia_semana BETWEEN 0 AND 6),
                                    hora_inicio TIME NOT NULL,
                                    hora_fin TIME NOT NULL,
                                    duracion_slot_min INTEGER NOT NULL,
                                    CHECK (hora_fin > hora_inicio)
);

-- reserva  (tu HU: Crear reserva)
CREATE TABLE reserva (
                         id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         usuario_id INTEGER NOT NULL REFERENCES usuario(id),
                         horario_id INTEGER NOT NULL REFERENCES horario_disponible(id),
                         fecha DATE NOT NULL,
                         hora TIME NOT NULL,
                         estado VARCHAR(20) NOT NULL DEFAULT 'activa' CHECK (estado IN ('activa', 'cancelada', 'completada')),
                         created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Evita dobles reservas activas sobre el mismo horario y fecha
-- (apoya directamente tu criterio de aceptación "Horario que ya está ocupado")
CREATE UNIQUE INDEX ux_reserva_horario_fecha_activa
    ON reserva (horario_id, fecha)
    WHERE estado = 'activa';