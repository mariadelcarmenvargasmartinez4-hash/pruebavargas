ALTER TABLE usuarios_acceso
    ADD COLUMN rol VARCHAR(20) NOT NULL DEFAULT 'CLIENTE';

ALTER TABLE usuarios_acceso
    ADD CONSTRAINT chk_usuarios_acceso_rol CHECK (rol IN ('CLIENTE', 'ADMIN'));