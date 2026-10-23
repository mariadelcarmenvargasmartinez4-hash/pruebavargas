-- Agregar columna de PIN transaccional cifrado para cuentas de clientes
ALTER TABLE cuentas ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(120);
