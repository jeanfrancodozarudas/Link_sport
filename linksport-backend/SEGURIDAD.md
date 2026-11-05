# Spring Security, autenticación, autorización y JWT

## Funciones implementadas

- Registro real de clientes en PostgreSQL mediante JPA. Correo normalizado, único sin distinguir mayúsculas y validación del servidor.
- Contraseñas con BCrypt (coste 12). Los DTOs, páginas y respuestas no muestran contraseñas ni hashes.
- Login real con mensaje genérico ante credenciales incorrectas. Los datos de login/registro ocultan la contraseña también en su representación para logs.
- JWT firmado con HS256, identificador único, emisor `linksport`, usuario, rol, versión y vencimiento de una hora.
- Cookie `LINKSPORT_ACCESS` HttpOnly y SameSite=Lax. El frontend no almacena JWT en localStorage ni sessionStorage. También se admiten peticiones con Authorization: Bearer para clientes de API.
- Spring Security valida firma, emisor y vencimiento. En cada petición autenticada se consultan la cuenta activa, su rol actual y la versión de revocación.
- Logout invalida todos los JWT anteriores del usuario e indica al navegador que elimine su cookie. Reutilizar un token revocado devuelve 401.
- Protección CSRF en todas las operaciones mutables, incluido login, registro, logout y compras de invitado. El frontend obtiene el token desde /api/auth/csrf y lo envía en X-XSRF-TOKEN; los formularios Thymeleaf reciben el campo CSRF.
- Visitantes: tienda, catálogo y compras/consulta de invitado. Clientes: su cuenta y su historial. Administradores: productos, inventario, pedidos y listado de usuarios.
- Las pantallas administrativas redirigen al login a los visitantes. Las API responden 401 sin autenticación y 403 ante permisos insuficientes o una comprobación CSRF fallida.
- Las compras realizadas con una cuenta iniciada se asocian al usuario del JWT. /api/pedidos/mios filtra siempre por ese usuario, nunca por un ID elegido por el cliente.
- El historial de cambios del pedido registra el rol e ID de quien hace la operación.

## Crear el administrador inicial

El registro público siempre crea CLIENTE. Para crear un administrador nuevo, define estas variables en la terminal de PowerShell antes de arrancar la aplicación, usando valores propios:

```powershell
$env:LINKSPORT_ADMIN_EMAIL = 'tu-correo@ejemplo.com'
$env:LINKSPORT_ADMIN_PASSWORD = 'REEMPLAZAR-POR-UNA-CLAVE-UNICA'
.\mvnw.cmd spring-boot:run
```

La contraseña inicial debe tener al menos 12 caracteres y un máximo de 72 bytes UTF-8. Cambia el ejemplo antes de ejecutar. El administrador se persiste; después de su creación puedes dejar ambas variables sin definir. Los arranques posteriores no cambian su contraseña. Si ese correo ya está registrado como cliente, la inicialización rechaza la configuración en lugar de promover una cuenta ajena. Elige otro correo inicial.

Para entrar, abre /login. La cuenta administradora abre /ver-productos y puede entrar a /admin/pedidos y /ver-usuarios.

## Clave JWT y HTTPS

LINKSPORT_JWT_SECRET acepta una clave aleatoria Base64 de al menos 32 bytes. Para generar una en PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
$env:LINKSPORT_JWT_SECRET = [Convert]::ToBase64String($bytes)
```

Conserva esa variable como secreto fuera del repositorio si quieres mantener los tokens válidos entre reinicios. Si no la defines, se genera una clave temporal nueva al arrancar y se deben iniciar nuevamente las sesiones.

Para una instalación con HTTPS, define LINKSPORT_COOKIE_SECURE=true. La configuración local permite HTTP en localhost; la cookie también se marca Secure cuando la petición ya llega mediante HTTPS.

## Compatibilidad con los datos anteriores

El esquema agrega rol, cuenta activa y versión de tokens. Las contraseñas de demostración guardadas anteriormente en texto plano se invalidan al iniciar: se desactiva esa cuenta y se reemplaza la contraseña por un hash aleatorio, conservando sus ventas. Los usuarios pueden registrarse con otro correo; la recuperación de contraseñas no está implementada.

Se quitó la cuenta de prueba con contraseña fija del archivo de datos iniciales. No hay promoción a administrador desde los formularios ni desde la API pública de registro.

## Endpoints principales

| Método | Ruta | Acceso |
|---|---|---|
| GET | /api/auth/csrf | Público |
| POST | /api/auth/registro | Público + CSRF |
| POST | /api/auth/login | Público + CSRF |
| GET | /api/auth/me | Autenticado |
| POST | /api/auth/logout | Autenticado + CSRF |
| GET | /api/pedidos/mios | Autenticado, solo sus compras |
| GET | /api/usuarios | ADMIN |
| POST / PUT / DELETE | /api/productos y /api/productos/{id} | ADMIN + CSRF |
| GET / PATCH | /api/admin/pedidos y sus subrutas | ADMIN; PATCH también requiere CSRF |

Las compras de invitado siguen disponibles. Un código privado de pedido sigue siendo una credencial de consulta de esa compra; no debe compartirse públicamente.

Las compras de cuenta no guardan códigos privados en el historial compartido del navegador. Solo las compras de invitado usan ese almacenamiento; las de cuenta se consultan desde su historial autenticado.

## Verificación

Resultado final: 37 pruebas Java y 25 pruebas JavaScript aprobadas. También se verificó que los formularios Thymeleaf incluyen CSRF y que las operaciones anteriores de inventario y pedidos siguen funcionando con las rutas protegidas.

Ejecutar `.\mvnw.cmd test` con Java disponible. El perfil de pruebas utiliza H2 en memoria y no modifica PostgreSQL. Ejecutar `node src/test/js/run.cjs` para las pruebas del frontend.

Las pruebas cubren registro persistente, hashing, login incorrecto, duplicados, roles, bloqueo de visitantes/clientes, CSRF incluso con JWT en cookie, firma manipulada, vencimiento, revocación, cuenta desactivada, cambios de rol e historial aislado por cuenta; además de los flujos anteriores de productos, inventario y pedidos.

Pendiente de comprobar en el entorno local definitivo: conexión y ajuste de esquema en PostgreSQL y recorrido visual interactivo en navegador. No se incluyen recuperación de contraseña ni verificación de correo/SMS.

Referencias técnicas: [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) y [protección CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).