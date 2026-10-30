package com.linksport.linksport_backend.controller;

import jakarta.validation.Valid;
import com.linksport.linksport_backend.model.Producto;
import com.linksport.linksport_backend.service.ProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<Producto> listarProductos() {
        return productoService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProducto(@PathVariable Long id) {
        Optional<Producto> p = productoService.buscarPorId(id);
        return p.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crearProducto(@Valid @RequestBody Producto producto) {
        // CORRECCIÓN INTERACTIVA: Comprueba si el nombre ya existe dentro de la MISMA
        // categoría
        boolean duplicadoEnMismaCategoria = productoService.obtenerTodos().stream()
                .anyMatch(p -> p.getNombre().equalsIgnoreCase(producto.getNombre())
                        && p.getCategoria() != null
                        && producto.getCategoria() != null
                        && p.getCategoria().getId().equals(producto.getCategoria().getId()));

        if (duplicadoEnMismaCategoria) {
            return ResponseEntity.badRequest().body("Error: Este producto ya está registrado en esta sección.");
        }

        Producto nuevo = productoService.guardar(producto);
        return ResponseEntity.ok(nuevo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> modificarProducto(@PathVariable Long id, @Valid @RequestBody Producto producto) {
        producto.setId(id);
        Producto actualizado = productoService.actualizar(id, producto);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> borrarProducto(@PathVariable Long id) {
        if (productoService.eliminar(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/procesar-pago")
    public ResponseEntity<Void> procesarPagoWeb(@RequestBody List<Producto> carrito) {
        productoService.registrarVentaDesdeWeb(carrito);
        return ResponseEntity.ok().build();
    }
}
