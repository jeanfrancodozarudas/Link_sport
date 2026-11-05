package com.linksport.linksport_backend.service;

import com.linksport.linksport_backend.model.*;
import com.linksport.linksport_backend.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductoServiceTest {
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final ProductoService service = new ProductoService(productos, categorias);
    private Producto datos() {
        var categoria = new Categoria("Running"); categoria.setId(1L);
        var p = new Producto(null, "Zapatillas", 19.9, "foto.jpg"); p.setCantidad(8); p.setCategoria(categoria);
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(productos.saveAndFlush(any(Producto.class))).thenAnswer(call -> call.getArgument(0));
        return p;
    }
    @Test void actualizaCategoriaStockDestacadoYActivo() {
        var original = datos(); original.setId(42L); original.setVersion(3L);
        when(productos.findByIdForUpdate(42L)).thenReturn(Optional.of(original));
        var cambios = datos(); cambios.setVersion(3L); cambios.setCantidad(12); cambios.setDestacado(true); cambios.setActivo(false);
        var guardado = service.actualizar(42L, cambios);
        assertEquals(12, guardado.getCantidad()); assertTrue(guardado.isDestacado()); assertFalse(guardado.isActivo());
        assertEquals(1L, guardado.getCategoria().getId());
    }
    @Test void rechazaEdicionQueSobrescribiriaStockReciente() {
        var original = datos(); original.setId(42L); original.setVersion(4L);
        when(productos.findByIdForUpdate(42L)).thenReturn(Optional.of(original));
        var cambios = datos(); cambios.setVersion(3L);
        assertThrows(ResponseStatusException.class, () -> service.actualizar(42L, cambios));
        verify(productos, never()).saveAndFlush(any());
    }
    @Test void retirarNoBorraElHistorialNiLaFila() {
        var original = datos(); original.setId(42L);
        when(productos.findByIdForUpdate(42L)).thenReturn(Optional.of(original));
        assertTrue(service.eliminar(42L)); assertFalse(original.isActivo());
        verify(productos, never()).deleteById(any());
    }
    @Test void rechazaNombreDuplicadoGlobalmente() {
        var p = datos(); when(productos.existsByNombreIgnoreCaseAndIdNot("Zapatillas", -1L)).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> service.guardar(p));
        verify(productos, never()).saveAndFlush(any());
    }
    @Test void categoriaEsObligatoriaYDebeExistir() {
        var sinCategoria = datos(); sinCategoria.setCategoria(null);
        assertThrows(ResponseStatusException.class, () -> service.guardar(sinCategoria));
        var p = datos(); when(categorias.findById(1L)).thenReturn(Optional.empty());
        final var inexistente = p;
        assertThrows(ResponseStatusException.class, () -> service.guardar(inexistente));
    }
}