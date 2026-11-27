-- V11: Campos reales para comprobantes oficiales de pago y control de no duplicidad de notificaciones por correo
ALTER TABLE pagos_servicios
    ADD COLUMN IF NOT EXISTS num_autorizacion VARCHAR(60),
    ADD COLUMN IF NOT EXISTS id_tx_proveedor VARCHAR(60),
    ADD COLUMN IF NOT EXISTS nombre_producto VARCHAR(150),
    ADD COLUMN IF NOT EXISTS nombre_servicio VARCHAR(100),
    ADD COLUMN IF NOT EXISTS moneda VARCHAR(5) DEFAULT 'MXN',
    ADD COLUMN IF NOT EXISTS confirmado_sin_cargo BOOLEAN,
    ADD COLUMN IF NOT EXISTS notificacion_enviada BOOLEAN NOT NULL DEFAULT FALSE;
