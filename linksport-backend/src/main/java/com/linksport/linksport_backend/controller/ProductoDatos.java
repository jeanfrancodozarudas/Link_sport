package com.linksport.linksport_backend.controller;

import com.linksport.linksport_backend.model.Producto;

public record ProductoDatos(Long id, String nombre, double precio, String imagenSrc, int cantidad,
        Long categoriaId, String categoriaNombre, boolean destacado, boolean activo, Long version) {
    public static ProductoDatos de(Producto p) {
        return new ProductoDatos(p.getId(), p.getNombre(), p.getPrecio(), p.getImagenSrc(), p.getCantidad(),
            p.getCategoria() == null ? null : p.getCategoria().getId(),
            p.getCategoria() == null ? null : p.getCategoria().getNombre(), p.isDestacado(), p.isActivo(), p.getVersion());
    }
}