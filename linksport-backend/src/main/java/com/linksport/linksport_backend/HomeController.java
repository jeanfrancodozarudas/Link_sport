package com.linksport.linksport_backend;

import com.linksport.linksport_backend.model.Producto;
import com.linksport.linksport_backend.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
    private final ProductoService productos;
    public HomeController(ProductoService productos) { this.productos = productos; }

    @InitBinder("producto")
    public void camposProducto(WebDataBinder binder) {
        binder.setAllowedFields("id", "version", "nombre", "precio", "imagenSrc", "cantidad", "categoria.id", "destacado", "activo");
    }
    @GetMapping("/")
    public String tienda(Model model) {
        var activos = productos.obtenerTodos().stream().filter(Producto::isActivo).toList();
        model.addAttribute("productos", activos.stream().filter(Producto::isDestacado).toList());
        model.addAttribute("productosRunning", activos.stream().filter(p -> categoria(p, "Running")).toList());
        model.addAttribute("productosFutbol", activos.stream().filter(p -> categoria(p, "Fútbol")).toList());
        model.addAttribute("productosAccesorios", activos.stream().filter(p -> categoria(p, "Accesorios")).toList());
        return "index";
    }
    private boolean categoria(Producto p, String nombre) {
        return p.getCategoria() != null && nombre.equalsIgnoreCase(p.getCategoria().getNombre());
    }
    private void cargarPanel(Model model) {
        if (model.getAttribute("producto") instanceof Producto p && p.getCategoria() == null) p.setCategoria(new com.linksport.linksport_backend.model.Categoria());
        model.addAttribute("productos", productos.obtenerTodos());
        model.addAttribute("categorias", productos.obtenerCategorias());
    }
    @GetMapping("/ver-productos")
    public String panel(Model model, @RequestParam(required = false) Long id) {
        model.addAttribute("producto", id == null ? new Producto() : productos.buscarPorId(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe.")));
        cargarPanel(model);
        return "admin-productos";
    }
    @PostMapping("/admin/productos/guardar")
    public String guardar(@Valid @ModelAttribute("producto") Producto producto, BindingResult result,
            Model model, RedirectAttributes flash) {
        if (!result.hasErrors()) {
            try {
                productos.guardar(producto);
                flash.addFlashAttribute("exito", "Producto guardado. El catálogo y las existencias fueron actualizados.");
                return "redirect:/ver-productos";
            } catch (ResponseStatusException error) {
                result.reject("producto", error.getReason());
            } catch (DataIntegrityViolationException error) {
                result.reject("producto", "No se pudo guardar: revisa si el nombre ya está registrado y recarga el producto.");
            }
        }
        cargarPanel(model);
        return "admin-productos";
    }
}