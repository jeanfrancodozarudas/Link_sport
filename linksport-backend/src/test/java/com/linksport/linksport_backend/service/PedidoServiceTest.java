package com.linksport.linksport_backend.service;

import com.linksport.linksport_backend.controller.SolicitudPedido;
import com.linksport.linksport_backend.model.*;
import com.linksport.linksport_backend.repository.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PedidoServiceTest {
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final VentaRepository ventas = mock(VentaRepository.class);
    private final PedidoService service = new PedidoService(productos, ventas, mock(UsuarioRepository.class));
    private final String clave = "12345678-1234-1234-1234-123456789abc";

    private SolicitudPedido pedido(BigDecimal total, List<SolicitudPedido.Item> items) {
        Comprador comprador = new Comprador();
        comprador.setNombre("Ana Pérez"); comprador.setCorreo("ana@example.com");
        comprador.setTelefono("987654321"); comprador.setCiudad("Lima, Lima, Surco");
        comprador.setDireccion("Avenida Primavera 123");
        return new SolicitudPedido(comprador, items, "CONTRA_ENTREGA", true, total, clave);
    }
    private void catalogo() {
        Producto producto = new Producto(42L, "Zapatillas", 19.90, "foto.jpg"); producto.setCantidad(20);
        when(productos.findByIdForUpdate(42L)).thenReturn(Optional.of(producto));
        when(ventas.findByClavePedido(clave)).thenReturn(Optional.empty());
        when(ventas.saveAndFlush(any(Venta.class))).thenAnswer(call -> {
            Venta venta = call.getArgument(0); venta.setId(7L); return venta;
        });
    }
    @Test void guardaDatosRealesYPrecioDelCatalogoSinMarcarPagado() {
        catalogo();
        var resultado = service.crear(pedido(new BigDecimal("39.80"), List.of(new SolicitudPedido.Item(42L, 2))));
        assertEquals(7L, resultado.pedidoId());
        assertEquals(0, new BigDecimal("39.80").compareTo(resultado.total()));
        assertEquals("PENDIENTE_PAGO", resultado.estado());
        verify(ventas).saveAndFlush(argThat(v -> v.getUsuario() == null
            && v.getComprador().getCorreo().equals("ana@example.com")
            && v.getDetalles().get(0).getPrecioUnitario() == 19.90));
    }
    @Test void rechazaTotalManipuladoSinGuardarVenta() {
        catalogo();
        assertThrows(ResponseStatusException.class, () -> service.crear(pedido(new BigDecimal("0.01"), List.of(new SolicitudPedido.Item(42L, 2)))));
        verify(ventas, never()).saveAndFlush(any());
    }
    @Test void rechazaProductoInexistenteSinGuardarVenta() {
        catalogo();
        assertThrows(ResponseStatusException.class, () -> service.crear(pedido(new BigDecimal("19.90"), List.of(new SolicitudPedido.Item(999L, 1)))));
        verify(ventas, never()).saveAndFlush(any());
    }
    @Test void rechazaLineasDuplicadas() {
        catalogo();
        assertThrows(ResponseStatusException.class, () -> service.crear(pedido(new BigDecimal("39.80"), List.of(new SolicitudPedido.Item(42L, 1), new SolicitudPedido.Item(42L, 1)))));
        verify(ventas, never()).saveAndFlush(any());
    }
    @Test void reintentoDevuelveMismoPedidoSinDuplicarlo() {
        Venta anterior = new Venta(); anterior.setId(7L); anterior.setTotalPagar(39.80);
        when(ventas.findByClavePedido(clave)).thenReturn(Optional.of(anterior));
        assertEquals(7L, service.crear(pedido(new BigDecimal("39.80"), List.of(new SolicitudPedido.Item(42L, 2)))).pedidoId());
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(productos);
    }
    @Test void rechazaStockInsuficienteSinGuardar() {
        catalogo(); productos.findByIdForUpdate(42L).orElseThrow().setCantidad(1);
        assertThrows(ResponseStatusException.class, () -> service.crear(pedido(new BigDecimal("39.80"), List.of(new SolicitudPedido.Item(42L, 2)))));
        verify(ventas, never()).saveAndFlush(any());
    }
    @Test void rechazaProductoRetiradoSinGuardar() {
        catalogo(); productos.findByIdForUpdate(42L).orElseThrow().setActivo(false);
        assertThrows(ResponseStatusException.class, () -> service.crear(pedido(new BigDecimal("39.80"), List.of(new SolicitudPedido.Item(42L, 2)))));
        verify(ventas, never()).saveAndFlush(any());
    }
    @Test void descuentaExactamenteLasUnidadesCompradas() {
        catalogo(); service.crear(pedido(new BigDecimal("39.80"), List.of(new SolicitudPedido.Item(42L, 2))));
        assertEquals(18, productos.findByIdForUpdate(42L).orElseThrow().getCantidad());
    }
    @Test void validaDatosCantidadesCarritoYConsentimiento() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var valido = pedido(new BigDecimal("39.80"), List.of(new SolicitudPedido.Item(42L, 2)));
            assertTrue(validator.validate(valido).isEmpty());
            assertFalse(validator.validate(pedido(BigDecimal.ONE, List.of())).isEmpty());
            assertFalse(validator.validate(pedido(BigDecimal.ONE, List.of(new SolicitudPedido.Item(42L, -1)))).isEmpty());
            assertFalse(validator.validate(pedido(BigDecimal.ONE, List.of(new SolicitudPedido.Item(42L, 100)))).isEmpty());
            valido.comprador().setCorreo("correo-invalido");
            assertFalse(validator.validate(valido).isEmpty());
            var sinConfirmar = new SolicitudPedido(valido.comprador(), valido.productos(), "TARJETA", false, BigDecimal.ONE, clave);
            var fallos = validator.validate(sinConfirmar);
            assertTrue(fallos.stream().anyMatch(v -> v.getPropertyPath().toString().equals("confirmado")));
            assertTrue(fallos.stream().anyMatch(v -> v.getPropertyPath().toString().equals("metodoPago")));
        }
    }
}