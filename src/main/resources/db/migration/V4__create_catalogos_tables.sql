-- Creacion de tablas de catalogos para desacoplar datos fijos
CREATE TABLE IF NOT EXISTS catalogo_sexos (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(10)     NOT NULL UNIQUE,
    descripcion         VARCHAR(50)     NOT NULL UNIQUE,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS catalogo_nacionalidades (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(10)     NOT NULL UNIQUE,
    descripcion         VARCHAR(100)    NOT NULL UNIQUE,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS catalogo_estados_civiles (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(20)     NOT NULL UNIQUE,
    descripcion         VARCHAR(50)     NOT NULL UNIQUE,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Indices para busqueda rapida en catalogos
CREATE INDEX IF NOT EXISTS idx_cat_sexos_codigo ON catalogo_sexos(codigo);
CREATE INDEX IF NOT EXISTS idx_cat_sexos_activo ON catalogo_sexos(activo);
CREATE INDEX IF NOT EXISTS idx_cat_nac_codigo ON catalogo_nacionalidades(codigo);
CREATE INDEX IF NOT EXISTS idx_cat_nac_activo ON catalogo_nacionalidades(activo);
CREATE INDEX IF NOT EXISTS idx_cat_est_codigo ON catalogo_estados_civiles(codigo);
CREATE INDEX IF NOT EXISTS idx_cat_est_activo ON catalogo_estados_civiles(activo);

-- Insercion de datos semilla normalizados sin espacios iniciales ni finales
INSERT INTO catalogo_sexos (codigo, descripcion, activo) VALUES
    ('MASC', 'MASCULINO', TRUE),
    ('FEM', 'FEMENINO', TRUE),
    ('NOBIN', 'NO BINARIO', TRUE),
    ('OTRO', 'OTRO', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO catalogo_nacionalidades (codigo, descripcion, activo) VALUES
    ('MEX', 'MEXICANA', TRUE),
    ('USA', 'ESTADOUNIDENSE', TRUE),
    ('CAN', 'CANADIENSE', TRUE),
    ('ESP', 'ESPAÑOLA', TRUE),
    ('COL', 'COLOMBIANA', TRUE),
    ('ARG', 'ARGENTINA', TRUE),
    ('OTRA', 'OTRA', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO catalogo_estados_civiles (codigo, descripcion, activo) VALUES
    ('SOL', 'SOLTERO', TRUE),
    ('CAS', 'CASADO', TRUE),
    ('DIV', 'DIVORCIADO', TRUE),
    ('VIU', 'VIUDO', TRUE),
    ('UNL', 'UNION LIBRE', TRUE)
ON CONFLICT (codigo) DO NOTHING;
