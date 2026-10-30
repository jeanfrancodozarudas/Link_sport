package com.linksport.linksport_backend.service;

import com.linksport.linksport_backend.model.Producto;
import com.linksport.linksport_backend.model.Venta;
import com.linksport.linksport_backend.model.DetalleVenta;
import com.linksport.linksport_backend.repository.ProductoRepository;
import com.linksport.linksport_backend.repository.VentaRepository;
import com.linksport.linksport_backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;

    // Capa de Servicio: Inyección por Constructor requerida para la rúbrica de la
    // UTP
    public ProductoService(ProductoRepository productoRepository,
            VentaRepository ventaRepository,
            UsuarioRepository usuarioRepository) {
        this.productoRepository = productoRepository;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ==========================================================================
    // MÉTODOS DEL CATALOGO DE PRODUCTOS (CRUD EN BASE DE DATO REAL)
    // ==========================================================================
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findById(id);
    }

    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    public Producto actualizar(Long id, Producto datosActualizados) {
        Optional<Producto> productoOpt = buscarPorId(id);
        if (productoOpt.isPresent()) {
            Producto p = productoOpt.get();
            p.setNombre(datosActualizados.getNombre());
            p.setPrecio(datosActualizados.getPrecio());
            p.setImagenSrc(datosActualizados.getImagenSrc());
            return productoRepository.save(p);
        }
        return null;
    }

    public boolean eliminar(Long id) {
        if (productoRepository.existsById(id)) {
            productoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // ==========================================================================
    // MOTOR MAESTRO: RECIBE EL CARRITO WEB Y LO PERSISTE EN POSTGRESQL
    // ==========================================================================
    public void registrarVentaDesdeWeb(List<Producto> carrito) {
        if (carrito == null || carrito.isEmpty())
            return;

        // 1. Buscamos al usuario de pruebas ID 1 que inyectamos en el import.sql
        var usuarioOpt = usuarioRepository.findById(1L);
        if (usuarioOpt.isEmpty())
            return;

        // 2. Instanciamos la cabecera principal de la Venta
        Venta nuevaVenta = new Venta();
        nuevaVenta.setUsuario(usuarioOpt.get());

        // Calculamos el monto acumulado del carrito enviado desde internet
        double totalCalculado = 0;
        for (Producto p : carrito) {
            totalCalculado += p.getPrecio() * p.getCantidad();
        }
        nuevaVenta.setTotalPagar(totalCalculado);

        // 3. Recorremos el carrito y poblamos la lista de detalles relacionales
        for (Producto p : carrito) {
            var productoRealOpt = productoRepository.findById(p.getId());
            if (productoRealOpt.isPresent()) {
                DetalleVenta detalle = new DetalleVenta();
                detalle.setVenta(nuevaVenta);
                detalle.setProducto(productoRealOpt.get());
                detalle.setCantidad(p.getCantidad());
                detalle.setPrecioUnitario(productoRealOpt.get().getPrecio());

                // Agregamos el item a la colección en cascada
                nuevaVenta.getDetalles().add(detalle);
            }
        }

        // 4. Se persisten ambas estructuras vinculadas en PostgreSQL de golpe
        ventaRepository.save(nuevaVenta);
    }
}
