-- Inserción de cliente de prueba demostrativo para pruebas en Swagger UI y Render
INSERT INTO clientes (id, nombre, apellido_paterno, apellido_materno, fecha_nacimiento, curp, rfc, sexo, nacionalidad, estado_civil, correo_electronico, telefono_movil, ocupacion, empresa, ingreso_mensual, activo)
VALUES (
    1,
    'JUAN',
    'PEREZ',
    'LOPEZ',
    '1990-05-15',
    'PELJ900515HDFRRN01',
    'PELJ9005151A0',
    'MASC',
    'MEX',
    'SOL',
    'juan.perez@prueba.com',
    '5512345678',
    'INGENIERO',
    'EMPRESA PRUEBA',
    35000.00,
    TRUE
)
ON CONFLICT (id) DO NOTHING;

-- Inserción de cuenta de prueba con saldo $1500.00 y PIN '1234' (BCrypt) para pruebas de Swagger en /pagos
INSERT INTO cuentas (id, cliente_id, numero_cuenta, clabe, tipo_cuenta, saldo, moneda, estatus, pin_hash)
VALUES (
    1,
    1,
    '1234567890123456',
    '012180123456789012',
    'DEBITO',
    1500.00,
    'MXN',
    'ACTIVA',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
)
ON CONFLICT (numero_cuenta) DO UPDATE 
SET saldo = 1500.00, 
    estatus = 'ACTIVA', 
    pin_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
