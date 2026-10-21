CREATE TABLE IF NOT EXISTS pagos_servicios (
    id                      BIGSERIAL PRIMARY KEY,
    folio_transaccion       VARCHAR(36)     NOT NULL UNIQUE,
    num_cuenta              VARCHAR(16)     NOT NULL,
    id_servicio             BIGINT          NOT NULL,
    id_producto             BIGINT          NOT NULL,
    id_concepto             BIGINT,
    id_distribuidor         INTEGER,
    monto                   DECIMAL(12, 2)  NOT NULL,
    upe                     VARCHAR(10)     NOT NULL,
    es_recarga_telefonica   BOOLEAN         NOT NULL,
    telefono                VARCHAR(15),
    referencia              VARCHAR(50),
    estatus                 VARCHAR(20)     NOT NULL DEFAULT 'APLICADO',
    fecha_operacion         TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pagos_num_cuenta ON pagos_servicios(num_cuenta);
CREATE INDEX IF NOT EXISTS idx_pagos_upe ON pagos_servicios(upe);
CREATE INDEX IF NOT EXISTS idx_pagos_folio ON pagos_servicios(folio_transaccion);
