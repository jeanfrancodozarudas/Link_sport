# Prioridades altas implementadas

## Inventario

- `Producto.cantidad` representa unidades disponibles para vender.
- El carrito muestra existencias e impide añadir más unidades que las visibles; los productos agotados no se pueden añadir.
- El servidor comprueba de nuevo stock y producto activo, y descuenta existencias en la misma transacción que crea el pedido.
- Compras, devoluciones por cancelación y ediciones bloquean filas de producto. Los bloqueos de varios productos se toman por ID ascendente para evitar interbloqueos entre carritos.
- La edición requiere la versión que se cargó en el formulario: no puede sobrescribir silenciosamente un stock que cambió por una compra.
- Un pedido cancelado devuelve sus existencias una sola vez. Los pedidos anteriores que nunca descontaron stock no lo incrementan al cancelarse.
- La clave única y una segunda comprobación tras adquirir bloqueos permiten reintentar sin duplicar compras ni descontar stock dos veces.

## Administración de productos: /ver-productos

El formulario permite crear y editar nombre, precio, imagen, categoría, existencias, destacado y activo. Los errores se muestran dentro de la página. La edición de un ID inexistente devuelve 404. El catálogo solo muestra productos activos; un nuevo producto se muestra en su categoría y, si se marca, entre los destacados.

Se mantiene una única regla de nombres: el nombre debe ser único en todo el catálogo, ignorando mayúsculas y espacios al principio/final. También se agrega un índice en PostgreSQL. Si una base preexistente tiene nombres duplicados que solo se diferencian por mayúsculas, deben corregirse antes de aplicar ese índice; no se borran registros automáticamente.

“Retirar” desactiva el producto sin eliminarlo ni romper detalles de ventas. Se reactiva desde Editar. La API devuelve DTOs con categoría simplificada, evitando serialización recursiva de entidades.

## Pedidos: /admin/pedidos

Listado paginado de 20 pedidos y filtro por estado de entrega. Cada detalle incluye comprador, dirección, productos, precios registrados al comprar, total e historial con fecha y observación.

Transiciones permitidas:

- REGISTRADO → PREPARANDO o CANCELADO.
- PREPARANDO → EN_CAMINO o CANCELADO.
- EN_CAMINO → ENTREGADO o CANCELADO.
- ENTREGADO y CANCELADO son estados finales.

El pago se gestiona por separado: PENDIENTE_PAGO, PAGADO o CANCELADO. Para marcar entregado se exige confirmar el cobro completo contra entrega. No se permite confirmar el cobro en otra etapa. Una actualización basada en un estado antiguo devuelve conflicto para que el operador recargue.

Cada cambio deja fecha, estados y observación. El historial ahora identifica el rol e ID de la cuenta que realiza el cambio. Ver SEGURIDAD.md.

## Consulta del comprador: /mis-pedidos

El recibo muestra un código privado aleatorio. El navegador conserva hasta 50 identificadores y códigos, sin copiar nombre, correo ni dirección al almacenamiento local. El comprador puede abrir el historial del navegador o introducir su código manualmente.

La consulta no acepta un ID correlativo como credencial. El código permite ver los datos de entrega del pedido: debe conservarse en privado. El historial asociado a una cuenta ya está disponible para las compras realizadas con sesión iniciada.

## Verificación

Se añadió un perfil `test` con H2 en memoria, solo para pruebas; no cambia la base PostgreSQL de la aplicación. Las pruebas cubren compras simultáneas, reintentos, stock insuficiente, total manipulado, cancelación repetida, devolución de stock de productos retirados, confirmación del cobro, edición obsoleta, renderizado de páginas y formularios/API.

Ejecutar con un JDK compatible con el nivel Java 25 del proyecto:

```
.\mvnw.cmd test
```

Pruebas JavaScript con Node:

```
node src/test/js/run.cjs
```

Resultado de verificación: 26 pruebas Java aprobadas (incluidas 11 de integración con H2 y HTTP) y 15 pruebas JavaScript aprobadas. Se verificaron también la sintaxis de todos los scripts, el formulario de creación/edición y las pantallas nuevas. Se utilizó el JDK 27 instalado en la PC, compilando para el nivel Java 25 del proyecto.

La verificación final con PostgreSQL y la revisión visual interactiva quedan pendientes. Las rutas administrativas ahora se protegen con Spring Security y rol ADMIN; consultar SEGURIDAD.md para crear el administrador y configurar la clave JWT.