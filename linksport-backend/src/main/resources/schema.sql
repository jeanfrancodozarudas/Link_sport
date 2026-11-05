-- Permite compras como invitado en bases que tenían la relación obligatoria.
ALTER TABLE IF EXISTS ventas ALTER COLUMN usuario_id DROP NOT NULL;

-- Nuevas columnas generadas por Hibernate antes de este ajuste.
UPDATE ventas SET estado = 'PENDIENTE_PAGO' WHERE estado IS NULL;
UPDATE ventas SET metodo_pago = 'CONTRA_ENTREGA' WHERE metodo_pago IS NULL;
UPDATE ventas SET estado_entrega = 'REGISTRADO' WHERE estado_entrega IS NULL;
UPDATE ventas SET stock_descontado = false WHERE stock_descontado IS NULL;
UPDATE productos SET activo = true WHERE activo IS NULL;
UPDATE productos SET version = 0 WHERE version IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_productos_nombre_normalizado ON productos (lower(trim(nombre)));
UPDATE detalles_ventas d SET nombre_producto = p.nombre FROM productos p
WHERE d.producto_id = p.id AND d.nombre_producto IS NULL;
UPDATE usuarios SET rol = 'CLIENTE' WHERE rol IS NULL;
UPDATE usuarios SET activo = true WHERE activo IS NULL;
UPDATE usuarios SET token_version = 0 WHERE token_version IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_correo_normalizado ON usuarios (lower(trim(correo)));