package com.linksport.linksport_backend.controller;

import jakarta.validation.Valid;
import com.linksport.linksport_backend.model.Producto;
import com.linksport.linksport_backend.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/productos")

public class ProductoController {
    private final ProductoService productos;
    public ProductoController(ProductoService productos) { this.productos = productos; }
    @GetMapping
    public List<ProductoDatos> listar() {
        return productos.obtenerTodos().stream().filter(Producto::isActivo).map(ProductoDatos::de).toList();
    }
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDatos> obtener(@PathVariable Long id) {
        return productos.buscarPorId(id).filter(Producto::isActivo).map(ProductoDatos::de)
            .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PostMapping
    public ProductoDatos crear(@Valid @RequestBody Producto datos) {
        datos.setId(null); datos.setVersion(null);
        return ProductoDatos.de(productos.guardar(datos));
    }
    @PutMapping("/{id}")
    public ProductoDatos actualizar(@PathVariable Long id, @Valid @RequestBody Producto datos) {
        return ProductoDatos.de(productos.actualizar(id, datos));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> retirar(@PathVariable Long id) {
        return productos.eliminar(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}