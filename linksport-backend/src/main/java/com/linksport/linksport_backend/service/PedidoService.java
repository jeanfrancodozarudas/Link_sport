package com.linksport.linksport_backend.service;

import com.linksport.linksport_backend.controller.*;
import com.linksport.linksport_backend.model.*;
import com.linksport.linksport_backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PedidoService {
    private final ProductoRepository productos;
    private final VentaRepository ventas;
    private final UsuarioRepository usuarios;
    public PedidoService(ProductoRepository productos, VentaRepository ventas, UsuarioRepository usuarios) {
        this.productos = productos; this.ventas = ventas; this.usuarios = usuarios;
    }
    @Transactional
    public Resultado crear(SolicitudPedido solicitud) {
        var existente = ventas.findByClavePedido(solicitud.clavePedido());
        if (existente.isPresent()) return resultado(existente.get());
        var ids = new HashSet<Long>();
        for (var item : solicitud.productos())
            if (!ids.add(item.id())) throw error(HttpStatus.BAD_REQUEST, "Producto repetido en el pedido.");
        // Todas las operaciones de inventario toman los bloqueos en el mismo orden.
        var bloqueados = new LinkedHashMap<Long, Producto>();
        for (Long id : ids.stream().sorted().toList())
            bloqueados.put(id, productos.findByIdForUpdate(id).orElseThrow(() ->
                error(HttpStatus.BAD_REQUEST, "Un producto ya no está disponible. Actualiza el carrito.")));
        // Otro reintento puede haber terminado mientras esperábamos las existencias.
        existente = ventas.findByClavePedido(solicitud.clavePedido());
        if (existente.isPresent()) return resultado(existente.get());
        Venta venta = new Venta();
        var autenticacion = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof com.linksport.linksport_backend.security.UsuarioActual actual)
            venta.setUsuario(usuarios.findById(actual.id()).orElseThrow());
        venta.setComprador(solicitud.comprador()); venta.setClavePedido(solicitud.clavePedido());
        BigDecimal total = BigDecimal.ZERO;
        for (var item : solicitud.productos()) {
            Producto producto = bloqueados.get(item.id());
            if (!producto.isActivo()) throw error(HttpStatus.CONFLICT, "El producto " + producto.getNombre() + " fue retirado del catálogo.");
            if (producto.getCantidad() < item.cantidad())
                throw error(HttpStatus.CONFLICT, "Stock insuficiente para " + producto.getNombre() + ". Disponibles: " + producto.getCantidad() + ". Ajusta el carrito.");
            if (!Double.isFinite(producto.getPrecio()) || producto.getPrecio() <= 0)
                throw error(HttpStatus.CONFLICT, "Un producto no tiene un precio válido.");
            BigDecimal precio = BigDecimal.valueOf(producto.getPrecio()).setScale(2, RoundingMode.HALF_UP);
            total = total.add(precio.multiply(BigDecimal.valueOf(item.cantidad())));
            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta); detalle.setProducto(producto); detalle.setNombreProducto(producto.getNombre());
            detalle.setCantidad(item.cantidad()); detalle.setPrecioUnitario(precio.doubleValue());
            venta.getDetalles().add(detalle);
        }
        if (total.compareTo(solicitud.totalConfirmado()) != 0)
            throw error(HttpStatus.CONFLICT, "Los precios cambiaron. Actualiza la página y revisa el pedido antes de confirmar.");
        for (var item : solicitud.productos()) {
            Producto producto = bloqueados.get(item.id());
            producto.setCantidad(producto.getCantidad() - item.cantidad());
        }
        venta.setTotalPagar(total.doubleValue()); venta.setStockDescontado(true);
        venta.getHistorial().add(new EventoPedido("REGISTRADO", "PENDIENTE_PAGO", "Pedido confirmado por el comprador. Existencias reservadas.",actor()));
        return resultado(ventas.saveAndFlush(venta));
    }
    @Transactional(readOnly = true)
    public Pagina listar(int pagina, EstadoEntrega estado) {
        var pageable = PageRequest.of(Math.max(0, pagina), 20, Sort.by(Sort.Direction.DESC, "id"));
        var page = estado == null ? ventas.findAll(pageable) : ventas.findByEstadoEntrega(estado, pageable);
        return new Pagina(page.getContent().stream().map(PedidoDatos::de).toList(), page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }
    @Transactional(readOnly = true)
    public Pagina mios(Long usuarioId, int pagina) {
        var page = ventas.findByUsuarioId(usuarioId, PageRequest.of(Math.max(0, pagina), 20, Sort.by(Sort.Direction.DESC,"id")));
        return new Pagina(page.getContent().stream().map(PedidoDatos::de).toList(),page.getNumber(),page.getTotalPages(),page.getTotalElements());
    }
    @Transactional(readOnly = true)
    public PedidoDatos detalle(Long id) { return PedidoDatos.de(buscar(id)); }

    @Transactional(readOnly = true)
    public PedidoDatos consultar(String clave) {
        return PedidoDatos.de(ventas.findByClavePedido(clave).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "No se encontró un pedido con ese código.")));
    }
    @Transactional
    public PedidoDatos cambiar(Long id, CambioPedido cambio) {
        Venta venta = ventas.findByIdForUpdate(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "El pedido no existe."));
        if (venta.getEstadoEntrega() == cambio.estadoEntrega()) return PedidoDatos.de(venta);
        if (venta.getEstadoEntrega() != cambio.estadoActual())
            throw error(HttpStatus.CONFLICT, "El pedido cambió en otra sesión. Recarga sus datos antes de continuar.");
        if (!venta.getEstadoEntrega().permite(cambio.estadoEntrega()))
            throw error(HttpStatus.CONFLICT, "Ese cambio de estado no está permitido.");
        if (cambio.estadoEntrega() == EstadoEntrega.ENTREGADO) {
            if (!cambio.cobroConfirmado()) throw error(HttpStatus.BAD_REQUEST, "Confirma que recibiste el pago contra entrega antes de completar el pedido.");
            venta.setEstado("PAGADO");
        } else if (cambio.cobroConfirmado()) {
            throw error(HttpStatus.BAD_REQUEST, "El cobro contra entrega solo se confirma al entregar el pedido.");
        }
        if (cambio.estadoEntrega() == EstadoEntrega.CANCELADO) {
            if (venta.isStockDescontado()) {
                for (var detalle : venta.getDetalles().stream().sorted(Comparator.comparing(d -> d.getProducto().getId())).toList()) {
                    Producto p = productos.findByIdForUpdate(detalle.getProducto().getId())
                        .orElseThrow(() -> error(HttpStatus.CONFLICT, "No se pudo recuperar el producto para devolver las existencias."));
                    if (p.getCantidad() > Integer.MAX_VALUE - detalle.getCantidad())
                        throw error(HttpStatus.CONFLICT, "Las existencias superarían el límite permitido.");
                    p.setCantidad(p.getCantidad() + detalle.getCantidad());
                }
                venta.setStockDescontado(false);
            }
            venta.setEstado("CANCELADO");
        }
        venta.setEstadoEntrega(cambio.estadoEntrega());
        venta.getHistorial().add(new EventoPedido(venta.getEstadoEntrega().name(), venta.getEstado(), cambio.observacion().trim(),actor()));
        ventas.flush();
        return PedidoDatos.de(venta);
    }
    private String actor() {
        var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth!=null && auth.getPrincipal() instanceof com.linksport.linksport_backend.security.UsuarioActual u ? u.rol()+" #"+u.id() : "COMPRADOR INVITADO";
    }
    private Venta buscar(Long id) {
        return ventas.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "El pedido no existe."));
    }
    private ResponseStatusException error(HttpStatus status, String mensaje) { return new ResponseStatusException(status, mensaje); }
    private Resultado resultado(Venta venta) {
        return new Resultado(venta.getId(), BigDecimal.valueOf(venta.getTotalPagar()), venta.getEstado(), venta.getMetodoPago(), venta.getEstadoEntrega(), venta.getClavePedido(), venta.getUsuario()!=null);
    }
    public record Resultado(Long pedidoId, BigDecimal total, String estado, String metodoPago, EstadoEntrega estadoEntrega, String claveConsulta, boolean cuentaAsociada) {}
    public record Pagina(List<PedidoDatos> pedidos, int pagina, int totalPaginas, long totalPedidos) {}
}