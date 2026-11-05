package com.linksport.linksport_backend.service;

import com.linksport.linksport_backend.controller.*;
import com.linksport.linksport_backend.model.*;
import com.linksport.linksport_backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class InventarioIntegrationTest {
    @Autowired org.springframework.core.env.Environment entorno;
    @Autowired PedidoService pedidos;
    @Autowired ProductoService catalogo;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;

    private Producto producto(int stock) {
        var categoria = categorias.saveAndFlush(new Categoria("Prueba-" + UUID.randomUUID()));
        var p = new Producto(null, "Producto-" + UUID.randomUUID(), 10.0, "foto.jpg");
        p.setCategoria(categoria); p.setCantidad(stock); return productos.saveAndFlush(p);
    }
    private SolicitudPedido pedido(Producto p, int cantidad, String clave) {
        var c = new Comprador(); c.setNombre("Ana Pérez"); c.setCorreo("ana@example.com");
        c.setTelefono("987654321"); c.setCiudad("Lima"); c.setDireccion("Avenida Primavera 123");
        return new SolicitudPedido(c, List.of(new SolicitudPedido.Item(p.getId(), cantidad)), "CONTRA_ENTREGA", true,
            BigDecimal.TEN.multiply(BigDecimal.valueOf(cantidad)), clave);
    }
    private CambioPedido cambio(EstadoEntrega actual, EstadoEntrega siguiente, boolean cobro) {
        return new CambioPedido(actual, siguiente, cobro, "Cambio de prueba");
    }
    @Test void dosComprasSimultaneasNoVendenLaMismaUltimaUnidad() throws Exception {
        var p = producto(1); var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> compra = () -> {
                ready.countDown(); start.await(10, TimeUnit.SECONDS);
                try { pedidos.crear(pedido(p, 1, UUID.randomUUID().toString())); return true; }
                catch (ResponseStatusException error) { assertEquals(409, error.getStatusCode().value()); return false; }
            };
            var uno = pool.submit(compra); var dos = pool.submit(compra);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            int exitos = (uno.get(20, TimeUnit.SECONDS) ? 1 : 0) + (dos.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, exitos); assertEquals(0, productos.findById(p.getId()).orElseThrow().getCantidad());
        }
    }
    @Test void cancelacionRestituyeUnaSolaVezEImpideReabrir() {
        var p = producto(5); var creado = pedidos.crear(pedido(p, 2, UUID.randomUUID().toString()));
        assertEquals(3, productos.findById(p.getId()).orElseThrow().getCantidad());
        var cancelacion = cambio(EstadoEntrega.REGISTRADO, EstadoEntrega.CANCELADO, false);
        pedidos.cambiar(creado.pedidoId(), cancelacion); pedidos.cambiar(creado.pedidoId(), cancelacion);
        assertEquals(5, productos.findById(p.getId()).orElseThrow().getCantidad());
        assertEquals("CANCELADO", pedidos.detalle(creado.pedidoId()).estadoPago());
        assertThrows(ResponseStatusException.class, () -> pedidos.cambiar(creado.pedidoId(), cambio(EstadoEntrega.CANCELADO, EstadoEntrega.PREPARANDO, false)));
    }
    @Test void entregaRequiereCobroYConservaHistorial() {
        var p = producto(2); var creado = pedidos.crear(pedido(p, 1, UUID.randomUUID().toString()));
        pedidos.cambiar(creado.pedidoId(), cambio(EstadoEntrega.REGISTRADO, EstadoEntrega.PREPARANDO, false));
        pedidos.cambiar(creado.pedidoId(), cambio(EstadoEntrega.PREPARANDO, EstadoEntrega.EN_CAMINO, false));
        assertThrows(ResponseStatusException.class, () -> pedidos.cambiar(creado.pedidoId(), cambio(EstadoEntrega.EN_CAMINO, EstadoEntrega.ENTREGADO, false)));
        assertEquals("PENDIENTE_PAGO", pedidos.detalle(creado.pedidoId()).estadoPago());
        var entregado = pedidos.cambiar(creado.pedidoId(), cambio(EstadoEntrega.EN_CAMINO, EstadoEntrega.ENTREGADO, true));
        assertEquals("PAGADO", entregado.estadoPago()); assertEquals(4, entregado.historial().size());
        assertTrue(entregado.siguientes().isEmpty());
    }
    @Test void totalInvalidoRevierteTodaLaCompra() {
        var p = producto(5); var solicitud = pedido(p, 2, UUID.randomUUID().toString());
        var manipulado = new SolicitudPedido(solicitud.comprador(), solicitud.productos(), "CONTRA_ENTREGA", true, BigDecimal.ONE, solicitud.clavePedido());
        assertThrows(ResponseStatusException.class, () -> pedidos.crear(manipulado));
        assertEquals(5, productos.findById(p.getId()).orElseThrow().getCantidad());
    }
    @Test void reintentosConcurrentesSoloDescuentanUnaVez() throws Exception {
        var p = producto(1); var solicitud = pedido(p, 1, UUID.randomUUID().toString());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var uno = pool.submit(() -> pedidos.crear(solicitud)); var dos = pool.submit(() -> pedidos.crear(solicitud));
            assertEquals(uno.get(20, TimeUnit.SECONDS).pedidoId(), dos.get(20, TimeUnit.SECONDS).pedidoId());
        }
        assertEquals(0, productos.findById(p.getId()).orElseThrow().getCantidad());
    }
    @Test void productoRetiradoConservaPedidoYPermiteDevolverStock() {
        var p = producto(5); var creado = pedidos.crear(pedido(p, 2, UUID.randomUUID().toString()));
        catalogo.eliminar(p.getId());
        assertFalse(productos.findById(p.getId()).orElseThrow().isActivo());
        assertEquals(p.getNombre(), pedidos.detalle(creado.pedidoId()).productos().get(0).nombre());
        pedidos.cambiar(creado.pedidoId(), cambio(EstadoEntrega.REGISTRADO, EstadoEntrega.CANCELADO, false));
        assertEquals(5, productos.findById(p.getId()).orElseThrow().getCantidad());
    }
    @Test void edicionAntiguaNoSobrescribeUnPedidoReciente() {
        var p = producto(5);
        var edicion = productos.findById(p.getId()).orElseThrow();
        pedidos.crear(pedido(p, 1, UUID.randomUUID().toString()));
        edicion.setCantidad(10);
        assertThrows(ResponseStatusException.class, () -> catalogo.actualizar(p.getId(), edicion));
        assertEquals(4, productos.findById(p.getId()).orElseThrow().getCantidad());
    }
    @Test void consultaNoAceptaUnIdDePedidoComoClave() {
        var p = producto(2); var clave = UUID.randomUUID().toString(); var creado = pedidos.crear(pedido(p, 1, clave));
        assertEquals(creado.pedidoId(), pedidos.consultar(clave).id());
        assertThrows(ResponseStatusException.class, () -> pedidos.consultar(creado.pedidoId().toString()));
    }
    @Test void pantallasYApiDeCatalogoRenderizanSinErrores() throws Exception {
        var p = producto(3);
        String base = "http://localhost:" + entorno.getProperty("local.server.port");
        try (var client = clienteAdmin()) {
            for (String ruta : List.of("/", "/ver-productos", "/ver-productos?id=" + p.getId(), "/admin/pedidos", "/mis-pedidos", "/api/productos", "/api/productos/" + p.getId())) {
                var request = com.linksport.linksport_backend.SecurityHttp.request(client, java.net.URI.create(base + ruta)).GET().build();
                var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), "No renderiza " + ruta + ": " + response.body());
                if (ruta.startsWith("/ver-productos")) assertTrue(response.body().contains("name=\"_csrf\""), "El formulario debe incluir CSRF.");
                if (ruta.startsWith("/api/")) assertFalse(response.body().contains("\"productos\""), "La categoría no debe serializar relaciones recursivas.");
            }
        }
    }
    @Test void formularioAdminCreaYEditaTodosLosCampos() throws Exception {
        var categoria = categorias.saveAndFlush(new Categoria("Categoría-" + UUID.randomUUID()));
        var nombre = "Nuevo-" + UUID.randomUUID();
        String base = "http://localhost:" + entorno.getProperty("local.server.port");
        try (var client = clienteAdmin()) {
            String campos = "nombre=" + nombre + "&precio=25.50&cantidad=8&imagenSrc=foto.jpg&categoria.id=" + categoria.getId() + "&destacado=true&activo=true";
            var request = com.linksport.linksport_backend.SecurityHttp.request(client, java.net.URI.create(base + "/admin/productos/guardar"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(campos)).build();
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(302, response.statusCode(), response.body());
            var p = productos.findAll().stream().filter(item -> nombre.equals(item.getNombre())).findFirst().orElseThrow();
            assertEquals(8, p.getCantidad()); assertTrue(p.isDestacado()); assertTrue(p.isActivo());
            assertEquals(categoria.getId(), p.getCategoria().getId());
            String cambios = "id=" + p.getId() + "&version=" + p.getVersion() + "&nombre=" + nombre
                + "&precio=30.00&cantidad=12&imagenSrc=otra.jpg&categoria.id=" + categoria.getId() + "&_destacado=on&_activo=on";
            request = com.linksport.linksport_backend.SecurityHttp.request(client, java.net.URI.create(base + "/admin/productos/guardar"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(cambios)).build();
            response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(302, response.statusCode(), response.body());
            var editado = productos.findById(p.getId()).orElseThrow();
            assertEquals(12, editado.getCantidad()); assertEquals(30.0, editado.getPrecio());
            assertFalse(editado.isDestacado()); assertFalse(editado.isActivo());
        }
    }
    @Test void apiGestionaYConsultaUnPedidoSinSerializarEntidadesRecursivas() throws Exception {
        var p = producto(2); String clave = UUID.randomUUID().toString();
        var creado = pedidos.crear(pedido(p, 1, clave));
        String base = "http://localhost:" + entorno.getProperty("local.server.port");
        try (var client = clienteAdmin()) {
            var request = com.linksport.linksport_backend.SecurityHttp.request(client, java.net.URI.create(base + "/api/admin/pedidos?estado=REGISTRADO")).GET().build();
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode()); assertTrue(response.body().contains("totalPedidos"));
            request = com.linksport.linksport_backend.SecurityHttp.request(client, java.net.URI.create(base + "/api/pedidos/consulta"))
                .header("Content-Type", "application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"clave\":\"" + clave + "\"}")).build();
            response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode()); assertTrue(response.body().contains("historial"));
            request = com.linksport.linksport_backend.SecurityHttp.request(client, java.net.URI.create(base + "/api/admin/pedidos/" + creado.pedidoId() + "/estado"))
                .header("Content-Type", "application/json").method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(
                    "{\"estadoActual\":\"REGISTRADO\",\"estadoEntrega\":\"CANCELADO\",\"cobroConfirmado\":false,\"observacion\":\"Cliente cancela\"}")).build();
            response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body()); assertTrue(response.body().contains("CANCELADO"));
            assertEquals(2, productos.findById(p.getId()).orElseThrow().getCantidad());
        }
    }
    @Autowired com.linksport.linksport_backend.security.TokenService tokens;
    @Autowired UsuarioRepository usuarios;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    private java.net.http.HttpClient clienteAdmin() {
        var u=new Usuario(null,"Admin de prueba","admin-"+UUID.randomUUID()+"@example.com",passwords.encode("Prueba-segura-123"));
        u.setRol(Rol.ADMIN); u=usuarios.saveAndFlush(u);
        return com.linksport.linksport_backend.SecurityHttp.autenticado(java.net.URI.create("http://localhost:"+entorno.getProperty("local.server.port")),tokens.emitir(u).getTokenValue());
    }
}