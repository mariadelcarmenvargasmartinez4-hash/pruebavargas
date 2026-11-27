-- Tabla para codigos temporales de recuperacion de contrasena (cifrados con BCrypt, un solo uso y limite de intentos)
CREATE TABLE IF NOT EXISTS recuperaciones_password (
    id                      BIGSERIAL PRIMARY KEY,
    usuario_id              BIGINT          NOT NULL,
    codigo_hash             VARCHAR(120)    NOT NULL,
    reset_token_hash        VARCHAR(120),
    intentos_fallidos       INT             NOT NULL DEFAULT 0,
    max_intentos            INT             NOT NULL DEFAULT 3,
    verificado              BOOLEAN         NOT NULL DEFAULT FALSE,
    usado                   BOOLEAN         NOT NULL DEFAULT FALSE,
    fecha_creacion          TIMESTAMP       NOT NULL DEFAULT NOW(),
    fecha_expiracion        TIMESTAMP       NOT NULL,
    fecha_expiracion_reset  TIMESTAMP,
    CONSTRAINT fk_recuperacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios_acceso(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_recuperacion_usuario_activo ON recuperaciones_password(usuario_id, usado);
