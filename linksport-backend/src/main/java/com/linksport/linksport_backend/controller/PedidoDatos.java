package com.linksport.linksport_backend.controller;

import com.linksport.linksport_backend.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDatos(Long id, LocalDateTime fecha, BigDecimal total, String estadoPago,
        EstadoEntrega estadoEntrega, String metodoPago, Comprador comprador,
        List<Linea> productos, List<EventoPedido> historial, List<EstadoEntrega> siguientes, boolean cuentaAsociada) {
    public record Linea(Long productoId, String nombre, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}
    public static PedidoDatos de(Venta venta) {
        return new PedidoDatos(venta.getId(), venta.getFechaCompra(), BigDecimal.valueOf(venta.getTotalPagar()),
            venta.getEstado(), venta.getEstadoEntrega(), venta.getMetodoPago(), venta.getComprador(),
            venta.getDetalles().stream().map(d -> new Linea(d.getProducto().getId(),
                d.getNombreProducto() == null ? d.getProducto().getNombre() : d.getNombreProducto(), d.getCantidad(),
                BigDecimal.valueOf(d.getPrecioUnitario()), BigDecimal.valueOf(d.getPrecioUnitario()).multiply(BigDecimal.valueOf(d.getCantidad())))).toList(),
            List.copyOf(venta.getHistorial()), java.util.Arrays.stream(EstadoEntrega.values()).filter(venta.getEstadoEntrega()::permite).toList(), venta.getUsuario()!=null);
    }
}