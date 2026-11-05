# Proceso de compra

El carrito abre un formulario de nombre, correo, celular peruano y dirección. Después muestra los productos, cantidades, dirección, envío y total para revisión. El comprador debe confirmar expresamente antes de registrar el pedido.

El único método habilitado es CONTRA_ENTREGA. Registrar un pedido no cobra dinero: se guarda con estado PENDIENTE_PAGO. No se solicitan números de tarjeta ni CVV. Para cobrar en línea falta integrar una pasarela y verificar su notificación de pago en el servidor.

El servidor valida los datos, acepta solo IDs reales y cantidades de 1 a 99, calcula los precios desde el catálogo y rechaza totales distintos al confirmado. La clave única del pedido permite repetir una solicitud sin crear otra venta. La compra de invitado guarda sus datos en la venta, sin asignarla al usuario de prueba.

La configuración ahora conserva la base de datos con ddl-auto=update. Requiere PostgreSQL y una base linksport_db, además de Java 25. En una base nueva el catálogo estará vacío: se puede cargar una sola vez src/main/resources/import.sql desde pgAdmin. No ejecutar ese archivo sobre datos ya cargados. Ajustar las credenciales de application.properties al entorno local.

Pruebas del servidor: .\mvnw.cmd -Dtest=PedidoServiceTest test
Pruebas del flujo JavaScript (Node 18 o posterior): node src/test/js/run.cjs

No se ha implementado verificación de identidad por correo/SMS ni comprobación de cobertura de reparto. La validación de los campos comprueba su formato. El envío tiene importe cero en este primer flujo, como en el carrito original.

Inventario, gestión de pedidos y consulta del comprador: ver MEJORAS_ALTA.md. Las pruebas Java ahora disponen del perfil test con H2; no requieren instalar PostgreSQL. La aplicación normal continúa usando PostgreSQL.
