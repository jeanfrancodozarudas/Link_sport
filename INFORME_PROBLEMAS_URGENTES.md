# Informe de revisión urgente — Link Sport

Fecha: 4 de noviembre de 2026.
> Actualización posterior: se implementaron las prioridades altas de inventario, gestión de pedidos, administración de productos y serialización del catálogo. El detalle está en linksport-backend/MEJORAS_ALTA.md. Java ya se detectó instalado y 26 pruebas del backend y 15 de JavaScript pasaron; la comprobación final con PostgreSQL sigue pendiente. El resto del informe conserva los hallazgos originales para referencia.

> Segunda actualización: se implementaron autenticación, autorización con roles y JWT, usuarios persistentes con BCrypt, CSRF, cierre de sesión con revocación e historial por cuenta. Ver linksport-backend/SEGURIDAD.md. Las descripciones originales de login simulado y administración abierta que siguen abajo corresponden al estado inicial del proyecto.

## Alcance y límites

Revisión del código local: plantillas Thymeleaf, HTML, CSS, Bulma, JavaScript, controladores, servicios, modelos, repositorios, configuración y pruebas. No se han ejecutado el servidor ni pruebas con PostgreSQL: Java estaba pendiente de instalación. Los problemas de comportamiento que requieren ejecutar la aplicación se indican como riesgos por comprobar. No se cuenta con la rúbrica oficial del curso; los comentarios del código que mencionan ArrayList no sustituyen esa rúbrica.

## Diagnóstico

La base tecnológica solicitada ya existe: HTML, CSS, Bulma y JavaScript en el frontend; Java, Spring Boot, MVC, Thymeleaf, validación, JPA y PostgreSQL en el backend. No es necesario cambiar de framework. Sin embargo, hay funciones de interfaz simuladas, usuarios desconectados de la base de datos y administración sin autenticación. Todavía no se puede considerar un sistema completo verificado.

## Prioridad crítica: antes de presentar el sistema como funcional

### 1. Inicio de sesión y registro simulados

Evidencia: src/main/resources/static/js/script.js, eventos de form-login-modal y form-registro-modal.

Ambos formularios solo ejecutan alert(), cierran el modal y limpian sus campos. No envían solicitudes al backend. No existe un endpoint de login ni una sesión autenticada.

Consecuencia: cualquier dato aparenta iniciar sesión y registrar una cuenta; el sistema no identifica al cliente.

Solución: conectar registro e inicio de sesión al servidor, validar campos, comprobar credenciales, mantener sesión y permitir cerrarla. Mostrar errores reales. Usar Spring Security y separar los permisos CLIENTE y ADMIN.

Aceptación: credenciales incorrectas se rechazan; el registro aparece en PostgreSQL; una sesión identifica al usuario y se invalida al salir.

### 2. Contraseñas expuestas y cuentas temporales

Evidencia: controller/UsuarioController.java, service/UsuarioService.java, model/Usuario.java y templates/admin-usuarios.html.

UsuarioService almacena cuentas en un ArrayList, crea una cuenta con contraseña fija y no utiliza UsuarioRepository. La API devuelve entidades Usuario incluyendo password. La plantilla de usuarios también contiene una columna que imprime contraseñas; no se encontró una ruta que prepare esa vista.

Consecuencia: las cuentas creadas por esa API desaparecen al reiniciar; las contraseñas pueden salir en las respuestas JSON. Existe un repositorio JPA de usuarios, pero el servicio usa otra fuente de datos.

Solución: persistir usuarios con JPA, almacenar hashes mediante PasswordEncoder y devolver DTOs sin contraseñas. Eliminar la columna y las credenciales fijas. Agregar @Valid al registro y garantizar correo único normalizado. Confirmar con la rúbrica si exige un ejercicio específico con ArrayList antes de retirar esa parte académica.

Aceptación: ningún listado o respuesta de registro devuelve contraseñas o hashes, y las cuentas sobreviven a un reinicio.

### 3. Administración y API de productos sin permisos

Evidencia: HomeController.java, controller/ProductoController.java y pom.xml.

No se encontró configuración de seguridad ni dependencia de Spring Security. Están disponibles rutas para guardar, modificar y eliminar productos sin comprobar el rol de quien las invoca. Los controladores de usuarios y productos permiten CORS con origen '*'.

Consecuencia: si la aplicación es accesible, una persona sin autenticar puede intentar modificar el catálogo. CORS no reemplaza la autorización.

Solución: proteger tanto las pantallas como los endpoints, permitir mutaciones solo a administradores, configurar protección CSRF cuando se utilicen sesiones y eliminar CORS abierto si frontend y backend comparten origen.

Aceptación: un visitante y un cliente no pueden crear, editar o borrar productos, aunque llamen directamente a la API.

### 4. Falta comprobar arranque, base de datos y compra completa

Evidencia: pom.xml, application.properties, CHECKOUT.md y pruebas existentes.

El proyecto requiere Java 25 y PostgreSQL local. Las credenciales están escritas en la configuración. No hay una prueba de integración aislada de PostgreSQL para demostrar todo el recorrido. Las pruebas Java nuevas del pedido todavía no se pudieron ejecutar por falta de Java. Las seis pruebas JavaScript del checkout sí pasaron, usando un entorno de DOM y red simulado: no equivalen a una prueba en navegador con el servidor.

Solución: instalar/configurar Java, verificar Maven y PostgreSQL, separar credenciales mediante variables de entorno y documentar un arranque reproducible. Verificar catálogo, registro, autenticación, pedido y persistencia tras reiniciar.

Aceptación: otra persona puede seguir el README y ejecutar el proyecto; una compra aparece con sus detalles en PostgreSQL.

## Prioridad alta: integridad del sistema de ventas

### 5. No existe control real de inventario

Evidencia: model/Producto.java, import.sql y service/PedidoService.java.

Producto tiene cantidad, pero el flujo de pedido no comprueba ni descuenta existencias. El carrito admite hasta 99 unidades y los datos iniciales asignan cantidad 1. Debe definirse si ese campo representa stock: no conviene asumirlo sin aclararlo en el modelo.

Solución: definir stock como concepto de inventario, validarlo en el servidor y reservar/descontar unidades dentro de una transacción con control de concurrencia. Definir cuándo se restituyen por cancelación.

Aceptación: no se venden más unidades que las disponibles, incluso con dos compras simultáneas.

### 6. Pedidos sin gestión posterior

Evidencia: controller/PedidoController.java, service/PedidoService.java y model/Venta.java.

Se puede crear un pedido contra entrega, pero no hay pantalla ni endpoints para listar pedidos, consultar sus detalles, consultar historial del cliente, cancelar o gestionar entrega y cobro. El estado actual comienza como PENDIENTE_PAGO.

Solución: añadir gestión de pedidos con estados definidos y permisos. Separar estado de entrega y estado de pago si ambos evolucionan por separado. Registrar quién hace cada cambio y evitar transiciones incoherentes.

Aceptación: el administrador encuentra el pedido registrado y puede gestionar su recorrido; el cliente solo consulta sus propias compras cuando haya autenticación.

### 7. Administración de productos incompleta e inconsistente

Evidencia: templates/admin-productos.html, HomeController.java, service/ProductoService.java y model/Producto.java.

El formulario no permite administrar categoría, existencias ni destacado. Un producto nuevo puede no aparecer en las secciones que muestra la tienda. La API de actualización cambia nombre, precio e imagen, pero no los otros atributos. La validación de duplicados intenta permitir nombres iguales en categorías distintas, mientras la columna nombre es única globalmente. El borrado de un producto ya vendido puede fallar por referencias desde detalles de ventas.

Solución: completar el formulario y unificar las reglas de creación y edición. Decidir la unicidad del nombre. Usar desactivación de productos con historial de ventas y mensajes claros ante conflictos. Validar también IDs inexistentes.

Aceptación: un producto creado desde el panel aparece en la categoría correcta, se edita de forma coherente y se conserva su historial al retirarlo del catálogo.

### 8. Riesgo de errores al serializar entidades

Evidencia: model/Producto.java, model/Categoria.java, controller/ProductoController.java y application.properties.

Las relaciones Producto.categoria y Categoria.productos son bidireccionales; la API devuelve entidades directamente. Con relaciones lazy y open-in-view=false hay riesgo de errores de carga o recorridos recursivos al convertirlas a JSON. Es un riesgo identificado en código, pendiente de reproducción en ejecución.

Solución: devolver DTOs con los campos necesarios y preparar los datos dentro de la capa de servicio.

Aceptación: GET de productos devuelve JSON estable sin recursión, errores 500 ni información innecesaria.

## Prioridad media: interfaz y calidad de la entrega

### 9. Funciones visuales sin comportamiento y mensajes falsos

Evidencia: templates/index.html y static/js/script.js.

El buscador no tiene lógica de búsqueda. La suscripción anuncia cupones enviados sin contactar con un backend o servicio de correo. Algunos enlaces apuntan a '#' o a secciones no correspondientes. El carrito solo vive en memoria y se pierde al recargar. El modal de login se abre automáticamente a los dos segundos.

Solución: conectar la búsqueda, implementar o retirar la promesa de envío de cupones, corregir navegación y conservar el carrito con validación posterior del servidor. Abrir login cuando el usuario lo solicite.

### 10. HTML defectuoso, recursos y código repetido

Evidencia: templates/index.html, templates/admin-productos.html y static/CSS/styles.css.

Hay etiquetas link de Font Awesome sin cierre correcto y un enlace a https://cloudflare.com usado como stylesheet. Las fuentes apuntan a googleapis.com/css2 en lugar del recurso de Google Fonts esperado. Hay reglas CSS repetidas, estilos inline extensos y uso de !important que dificultan mantener Bulma. No se ha comprobado aún el aspecto en móvil ni en una sesión real del navegador.

Solución: validar HTML, corregir recursos y enlaces, consolidar CSS y comprobar navegación por teclado, etiquetas de formularios, contraste y vistas móviles.

### 11. Inserción de datos mediante innerHTML

Evidencia: static/js/script.js, renderizado del carrito y mensaje de suscripción.

El mensaje de suscripción interpola el nombre y el correo introducidos por el usuario dentro de HTML. El carrito también interpola datos de productos en HTML. Eso permite interpretar contenido como etiquetas en vez de texto y puede introducir ejecución de scripts mediante atributos si se reciben entradas maliciosas.

Solución: crear nodos y usar textContent para datos dinámicos; validar los orígenes de imágenes. Añadir pruebas con texto que contenga etiquetas.

### 12. Documentación y pruebas insuficientes para una entrega

Falta un README principal con requisitos, instalación de PostgreSQL, variables de entorno, carga inicial del catálogo, arranque y casos de demostración. Faltan pruebas de autenticación, permisos, CRUD, inventario y persistencia. La prueba contextLoads depende del entorno de base de datos.

Solución: preparar instrucciones reproducibles, diagrama de entidades y una lista de casos funcionales con sus resultados. Añadir pruebas que cubran las reglas principales y una configuración de pruebas aislada.

## Estado de los cambios ya realizados en esta sesión

- Implementado formulario de compra con datos de contacto y entrega, revisión del pedido y confirmación expresa.
- Registro contra entrega con número de pedido y estado PENDIENTE_PAGO. No hay cobro real ni pasarela conectada.
- Servidor calcula precios desde el catálogo y valida los datos del pedido, las cantidades y el total confirmado.
- Se dejó de asociar cada compra al usuario de prueba y se guardan los datos del comprador invitado.
- Se añadió una clave única para reintentar sin duplicar el pedido y el carrito se conserva ante errores.
- Se sustituyó create-drop por update para conservar datos y se añadió un ajuste de esquema para permitir compras como invitado. Falta verificarlo contra PostgreSQL.
- Seis pruebas JavaScript aprobadas. Pruebas Java y verificación completa pendientes.

## Orden recomendado de implementación

1. Autenticación real, usuarios persistentes y contraseñas protegidas.
2. Permisos de administrador y cliente para pantallas y API.
3. Arranque comprobado con PostgreSQL y pruebas del proceso de compra.
4. Inventario y administración completa del catálogo.
5. Panel de pedidos e historial del cliente.
6. Búsqueda, formularios, navegación y correcciones de HTML/CSS.
7. Pruebas de integración, documentación y ensayo de demostración.

Para una demostración académica, el pago contra entrega puede cubrir el recorrido de compra si la rúbrica lo permite. Una pasarela real, correos de verificación o SMS no son requisitos confirmados del curso; deben priorizarse según la rúbrica, después de los flujos básicos funcionales.