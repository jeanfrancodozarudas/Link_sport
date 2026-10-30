package com.linksport.linksport_backend;

import com.linksport.linksport_backend.model.Producto;
import com.linksport.linksport_backend.service.ProductoService;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;

@Controller
public class HomeController {

    private final ProductoService productoService;

    public HomeController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/")
    public String mostrarTienda(Model model) {
        List<Producto> todos = productoService.obtenerTodos();

        // FILTRO PREMIUM: Jala de forma inteligente los productos marcados como
        // destacados
        List<Producto> destacados = todos.stream().filter(Producto::isDestacado).toList();

        // Sigue filtrando las secciones inferiores de manera limpia por su ID
        // relacional
        List<Producto> running = todos.stream().filter(p -> p.getCategoria() != null && p.getCategoria().getId() == 1L)
                .toList();
        List<Producto> futbol = todos.stream().filter(p -> p.getCategoria() != null && p.getCategoria().getId() == 2L)
                .toList();
        List<Producto> accesorios = todos.stream()
                .filter(p -> p.getCategoria() != null && p.getCategoria().getId() == 3L).toList();

        // Inyectamos las colecciones dinámicas desacopladas hacia Thymeleaf
        model.addAttribute("productos", destacados);
        model.addAttribute("productosRunning", running);
        model.addAttribute("productosFutbol", futbol);
        model.addAttribute("productosAccesorios", accesorios);

        return "index";
    }

    @GetMapping("/ver-productos")
    public String verProductosPanel(Model model, @RequestParam(value = "id", required = false) Long id) {
        if (id != null) {
            // Si el link trae un ID, cargamos el producto existente en las cajas
            productoService.buscarPorId(id).ifPresent(p -> model.addAttribute("producto", p));
        } else {
            // Si no, cargamos uno en blanco limpio
            model.addAttribute("producto", new Producto());
        }
        model.addAttribute("productos", productoService.obtenerTodos());
        return "admin-productos";
    }

    @PostMapping("/admin/productos/guardar")
    public String guardarProductoDesdeWeb(@Valid @ModelAttribute("producto") Producto producto,
            BindingResult result, Model model) {

        // CORRECCIÓN INTERACTIVA: Comprueba duplicados de nombre SOLO si pertenecen a
        // la misma categoría
        if (producto.getId() == null) {
            boolean duplicadoEnMismaCategoria = productoService.obtenerTodos().stream()
                    .anyMatch(p -> p.getNombre().equalsIgnoreCase(producto.getNombre())
                            && p.getCategoria() != null
                            && producto.getCategoria() != null
                            && p.getCategoria().getId().equals(producto.getCategoria().getId()));

            if (duplicadoEnMismaCategoria) {
                result.rejectValue("nombre", "error.producto",
                        "Ya existe un artículo registrado en esta sección con este nombre.");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute("productos", productoService.obtenerTodos());
            return "admin-productos";
        }

        productoService.guardar(producto);
        return "redirect:/ver-productos";
    }
}
