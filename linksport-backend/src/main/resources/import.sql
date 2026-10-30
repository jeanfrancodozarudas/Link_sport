-- 1. REGISTRO DE CATEGORÍAS EN POSTGRESQL
INSERT INTO categorias (nombre) VALUES ('Running');        -- ID 1
INSERT INTO categorias (nombre) VALUES ('Fútbol');         -- ID 2
INSERT INTO categorias (nombre) VALUES ('Accesorios');     -- ID 3

-- 2. BLOQUE: COLECCIÓN RUNNING (Asociados al categoria_id = 1)
-- Los últimos dos campos corresponden a: cantidad, categoria_id, destacado
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Zapatillas Running Speed Pro', 249.90, 'imagenes/producto1.jpg', 1, 1, true); -- Destacado
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Pantalón Jogger Training Fit', 120.00, 'imagenes/producto2.jpg', 1, 1, false);
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Casaca Cortaviento Impermeable', 180.00, 'imagenes/producto3.jpg', 1, 1, false);
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Polo Técnico Dry-Fit Pro', 45.00, 'imagenes/producto4.jpg', 1, 1, true); -- Destacado

-- 3. BLOQUE: UNIVERSO FÚTBOL (Asociados al categoria_id = 2)
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Chimpunes Predator Elite FG', 389.00, 'imagenes/producto5.jpg', 1, 2, true); -- Destacado
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Camiseta Oficial Linksport FC', 119.90, 'imagenes/producto6.jpg', 1, 2, true); -- Destacado
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Short Oficial de Competencia', 69.90, 'imagenes/producto7.jpg', 1, 2, false);
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Medias Técnicas de Compresión', 25.00, 'imagenes/producto8.jpg', 1, 2, false);

-- 4. BLOQUE: ACCESORIOS DEPORTIVOS (Asociados al categoria_id = 3)
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Mochila Deportiva Waterproof', 145.00, 'imagenes/producto9.jpg', 1, 3, false);
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Tomatodo Térmico Inox 1L', 55.00, 'imagenes/producto10.jpg', 1, 3, false);
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Gorra Training Aeroready', 49.00, 'imagenes/producto11.jpg', 1, 3, false);
INSERT INTO productos (nombre, precio, imagen_src, cantidad, categoria_id, destacado) VALUES ('Guantes de Gimnasio Grip Pro', 35.00, 'imagenes/producto12.jpg', 1, 3, false);

-- 5. REGISTRO DE USUARIOS BASE
INSERT INTO usuarios (nombre, correo, password) VALUES ('Juan Pérez Linksport', 'juan@linksport.com', '123456');


