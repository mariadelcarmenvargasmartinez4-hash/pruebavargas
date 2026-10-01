-- Creacion de tablas para clientes, domicilios, cuentas bancarias y usuarios de acceso en PostgreSQL
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
    telefono_movil          VARCHAR(10)     NOT NULL,
    telefono_alternativo    VARCHAR(15),
    ocupacion               VARCHAR(100)    NOT NULL,
    empresa                 VARCHAR(100)    NOT NULL,
    ingreso_mensual         DECIMAL(12, 2)  NOT NULL,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW(),
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS domicilios (
    id                      BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT          NOT NULL UNIQUE,
    calle                   VARCHAR(100)    NOT NULL,
    numero_exterior         VARCHAR(20)     NOT NULL,
    numero_interior         VARCHAR(20),
    colonia                 VARCHAR(100)    NOT NULL,
    municipio               VARCHAR(100)    NOT NULL,
    estado                  VARCHAR(50)     NOT NULL,
    codigo_postal           VARCHAR(5)      NOT NULL,
    pais                    VARCHAR(50)     NOT NULL,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_domicilio_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS cuentas (
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

CREATE TABLE IF NOT EXISTS usuarios_acceso (
    id                      BIGSERIAL PRIMARY KEY,
    cliente_id              BIGINT          NOT NULL UNIQUE,
    username                VARCHAR(100)    NOT NULL UNIQUE,
    password_hash           VARCHAR(120)    NOT NULL,
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_actualizacion     TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_usuario_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_clientes_curp ON clientes(curp);
CREATE INDEX IF NOT EXISTS idx_clientes_rfc ON clientes(rfc);
CREATE INDEX IF NOT EXISTS idx_clientes_correo ON clientes(correo_electronico);
CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre);
CREATE INDEX IF NOT EXISTS idx_clientes_paterno ON clientes(apellido_paterno);
CREATE INDEX IF NOT EXISTS idx_clientes_materno ON clientes(apellido_materno);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo);
CREATE INDEX IF NOT EXISTS idx_domicilios_cliente ON domicilios(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_numero ON cuentas(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_cuentas_cliente_id ON cuentas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cuentas_estatus ON cuentas(estatus);
CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios_acceso(username);
CREATE INDEX IF NOT EXISTS idx_usuarios_cliente_id ON usuarios_acceso(cliente_id);
