package com.linksport.linksport_backend.controller;

import com.linksport.linksport_backend.model.EstadoEntrega;
import com.linksport.linksport_backend.service.PedidoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class GestionPedidoController {
    private final PedidoService pedidos;
    public GestionPedidoController(PedidoService pedidos) { this.pedidos = pedidos; }
    @GetMapping("/api/admin/pedidos")
    public PedidoService.Pagina listar(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(required = false) EstadoEntrega estado) { return pedidos.listar(pagina, estado); }
    @GetMapping("/api/admin/pedidos/{id}")
    public PedidoDatos detalle(@PathVariable Long id) { return pedidos.detalle(id); }
    @PatchMapping("/api/admin/pedidos/{id}/estado")
    public PedidoDatos cambiar(@PathVariable Long id, @Valid @RequestBody CambioPedido cambio) { return pedidos.cambiar(id, cambio); }
    @GetMapping("/api/pedidos/mios")
    public PedidoService.Pagina mios(@org.springframework.security.core.annotation.AuthenticationPrincipal com.linksport.linksport_backend.security.UsuarioActual actual,
            @RequestParam(defaultValue="0") int pagina) { return pedidos.mios(actual.id(),pagina); }
    // La consulta del comprador requiere un código aleatorio, nunca un ID correlativo.
    @PostMapping("/api/pedidos/consulta")
    public PedidoDatos consultar(@Valid @RequestBody Consulta consulta) { return pedidos.consultar(consulta.clave()); }
    public record Consulta(@NotBlank @Pattern(regexp = "[a-fA-F0-9-]{36}") String clave) {}
}