-- Migracion V9: Creacion de catalogo de entidades federativas y actualizacion de clientes
CREATE TABLE IF NOT EXISTS catalogo_entidades_federativas (
    id                  BIGSERIAL PRIMARY KEY,
    codigo              VARCHAR(10)     NOT NULL UNIQUE,
    descripcion         VARCHAR(100)    NOT NULL UNIQUE,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cat_ent_codigo ON catalogo_entidades_federativas(codigo);
CREATE INDEX IF NOT EXISTS idx_cat_ent_activo ON catalogo_entidades_federativas(activo);

-- Insercion de las 32 entidades federativas oficiales de Mexico con sus claves oficiales de RENAPO
INSERT INTO catalogo_entidades_federativas (codigo, descripcion, activo) VALUES
    ('AS', 'AGUASCALIENTES', TRUE),
    ('BC', 'BAJA CALIFORNIA', TRUE),
    ('BS', 'BAJA CALIFORNIA SUR', TRUE),
    ('CC', 'CAMPECHE', TRUE),
    ('CL', 'COAHUILA', TRUE),
    ('CM', 'COLIMA', TRUE),
    ('CS', 'CHIAPAS', TRUE),
    ('CH', 'CHIHUAHUA', TRUE),
    ('DF', 'CIUDAD DE MEXICO', TRUE),
    ('DG', 'DURANGO', TRUE),
    ('GT', 'GUANAJUATO', TRUE),
    ('GR', 'GUERRERO', TRUE),
    ('HG', 'HIDALGO', TRUE),
    ('JC', 'JALISCO', TRUE),
    ('MC', 'ESTADO DE MEXICO', TRUE),
    ('MN', 'MICHOACAN', TRUE),
    ('MS', 'MORELOS', TRUE),
    ('NT', 'NAYARIT', TRUE),
    ('NL', 'NUEVO LEON', TRUE),
    ('OC', 'OAXACA', TRUE),
    ('PL', 'PUEBLA', TRUE),
    ('QT', 'QUERETARO', TRUE),
    ('QR', 'QUINTANA ROO', TRUE),
    ('SP', 'SAN LUIS POTOSI', TRUE),
    ('SL', 'SINALOA', TRUE),
    ('SR', 'SONORA', TRUE),
    ('TC', 'TABASCO', TRUE),
    ('TS', 'TAMAULIPAS', TRUE),
    ('TL', 'TLAXCALA', TRUE),
    ('VZ', 'VERACRUZ', TRUE),
    ('YN', 'YUCATAN', TRUE),
    ('ZS', 'ZACATECAS', TRUE),
    ('NE', 'NACIDO EN EL EXTRANJERO', TRUE)
ON CONFLICT (codigo) DO NOTHING;

-- Agregar columna entidad_nacimiento a la tabla clientes para desacoplar nacimiento de domicilio
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS entidad_nacimiento VARCHAR(10) DEFAULT 'DF';

-- Ampliar longitud de nombre para soportar nombres completos compuestos
ALTER TABLE clientes ALTER COLUMN nombre TYPE VARCHAR(100);
