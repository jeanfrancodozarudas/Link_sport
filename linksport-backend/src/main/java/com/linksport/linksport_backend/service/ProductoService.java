package com.linksport.linksport_backend.service;

import com.linksport.linksport_backend.model.*;
import com.linksport.linksport_backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ProductoService {
    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    public ProductoService(ProductoRepository productos, CategoriaRepository categorias) {
        this.productos = productos; this.categorias = categorias;
    }
    public List<Producto> obtenerTodos() { return productos.findAll(); }
    public List<Categoria> obtenerCategorias() { return categorias.findAll(); }
    public Optional<Producto> buscarPorId(Long id) { return productos.findById(id); }

    @Transactional
    public Producto guardar(Producto datos) {
        return persistir(datos.getId(), datos);
    }
    @Transactional
    public Producto actualizar(Long id, Producto datos) { return persistir(id, datos); }

    private Producto persistir(Long id, Producto datos) {
        Producto p = id == null ? new Producto() : productos.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe."));
        if (id != null && (datos.getVersion() == null || !Objects.equals(p.getVersion(), datos.getVersion())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El inventario cambió. Recarga el producto antes de guardar para no sobrescribir existencias.");
        String nombre = datos.getNombre() == null ? "" : datos.getNombre().trim();
        if (productos.existsByNombreIgnoreCaseAndIdNot(nombre, id == null ? -1L : id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un producto con ese nombre. Usa un nombre distinto.");
        if (datos.getCategoria() == null || datos.getCategoria().getId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una categoría.");
        Categoria categoria = categorias.findById(datos.getCategoria().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no existe."));
        if (nombre.length() < 3 || nombre.length() > 100 || !Double.isFinite(datos.getPrecio()) || datos.getPrecio() < 1 || datos.getCantidad() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisa nombre, precio y existencias.");
        p.setNombre(nombre); p.setPrecio(datos.getPrecio()); p.setImagenSrc(datos.getImagenSrc().trim());
        p.setCantidad(datos.getCantidad()); p.setCategoria(categoria);
        p.setDestacado(datos.isDestacado()); p.setActivo(datos.isActivo());
        return productos.saveAndFlush(p);
    }
    // Retirar conserva los detalles de ventas y permite reactivar desde el panel.
    @Transactional
    public boolean eliminar(Long id) {
        var encontrado = productos.findByIdForUpdate(id);
        if (encontrado.isEmpty()) return false;
        encontrado.get().setActivo(false);
        productos.flush();
        return true;
    }
}