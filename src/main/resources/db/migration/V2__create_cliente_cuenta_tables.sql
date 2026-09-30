-- Creacion de tablas para clientes y cuentas bancarias en PostgreSQL
CREATE TABLE IF NOT EXISTS clientes (
    id                      BIGSERIAL PRIMARY KEY,
    nombre                  VARCHAR(50)     NOT NULL,
    segundo_nombre          VARCHAR(50),
    apellido_paterno        VARCHAR(50)     NOT NULL,
    apellido_materno        VARCHAR(50)     NOT NULL,
    fecha_nacimiento        DATE            NOT NULL,
    curp                    VARCHAR(18)     NOT NULL UNIQUE,
    rfc                     VARCHAR(13)     NOT NULL UNIQUE,
    sexo                    VARCHAR(10)     NOT NULL,
    nacionalidad            VARCHAR(50)     NOT NULL,
    estado_civil            VARCHAR(20)     NOT NULL,
    correo_electronico      VARCHAR(100)    NOT NULL UNIQUE,
    telefono_movil          VARCHAR(15)     NOT NULL,
    telefono_alternativo    VARCHAR(15),
    calle                   VARCHAR(100)    NOT NULL,
    numero_exterior         VARCHAR(20)     NOT NULL,
    numero_interior         VARCHAR(20),
    colonia                 VARCHAR(100)    NOT NULL,
    municipio               VARCHAR(100)    NOT NULL,
    estado                  VARCHAR(50)     NOT NULL,
    codigo_postal           VARCHAR(5)      NOT NULL,
    pais                    VARCHAR(50)     NOT NULL,
    ocupacion               VARCHAR(100)    NOT NULL,
    empresa                 VARCHAR(100)    NOT NULL,
    ingreso_mensual         DECIMAL(12, 2)  NOT NULL,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW(),
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS cuentas_bancarias (
    id                      BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT          NOT NULL,
    numero_cuenta           VARCHAR(16)     NOT NULL UNIQUE,
    clabe                   VARCHAR(18)     NOT NULL UNIQUE,
    tipo_cuenta             VARCHAR(30)     NOT NULL DEFAULT 'DEBITO',
    saldo                   DECIMAL(14, 2)  NOT NULL DEFAULT 0.00,
    moneda                  VARCHAR(3)      NOT NULL DEFAULT 'MXN',
    estatus                 VARCHAR(20)     NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_clientes_curp ON clientes(curp);
CREATE INDEX IF NOT EXISTS idx_clientes_rfc ON clientes(rfc);
CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes(correo_electronico);
CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas_bancarias(cliente_id);
