-- Agregar columna de PIN transaccional cifrado para cuentas de clientes
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(120);

-- Inicializar cuentas existentes con un PIN por defecto hasheado con BCrypt (PIN "1234") para evitar cuentas sin autenticación
UPDATE cuentas
SET pin_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE pin_hash IS NULL;
